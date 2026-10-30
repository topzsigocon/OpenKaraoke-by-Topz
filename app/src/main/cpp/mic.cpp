// Mic -> echo + reverb -> speaker/headphones, gamit ang Oboe (low latency).
// Hiwalay ito sa synth: ang synth ay lumalabas sa AudioTrack at hinahalo ng Android ang dalawa.
#include <jni.h>
#include <oboe/Oboe.h>

#include <atomic>
#include <cmath>
#include <cstring>
#include <memory>
#include <vector>

namespace {

// ---- Mga simpleng effect (Schroeder/Freeverb-style) ----

class Comb {
public:
    void init(int size, float feedback, float damp) {
        buf.assign(size > 1 ? size : 1, 0.0f);
        idx = 0; fb = feedback; d = damp; store = 0.0f;
    }
    float process(float in) {
        float out = buf[idx];
        store = out * (1.0f - d) + store * d;
        buf[idx] = in + store * fb;
        if (++idx >= (int)buf.size()) idx = 0;
        return out;
    }
private:
    std::vector<float> buf; int idx = 0; float fb = 0.84f, d = 0.2f, store = 0.0f;
};

class Allpass {
public:
    void init(int size) { buf.assign(size > 1 ? size : 1, 0.0f); idx = 0; }
    float process(float in) {
        float b = buf[idx];
        float out = -in + b;
        buf[idx] = in + b * 0.5f;
        if (++idx >= (int)buf.size()) idx = 0;
        return out;
    }
private:
    std::vector<float> buf; int idx = 0;
};

class Echo {
public:
    void init(int size, float feedback) {
        buf.assign(size > 1 ? size : 1, 0.0f);
        idx = 0; fb = feedback;
    }
    float process(float in) {
        float out = buf[idx];
        buf[idx] = in + out * fb;
        if (++idx >= (int)buf.size()) idx = 0;
        return out;
    }
private:
    std::vector<float> buf; int idx = 0; float fb = 0.4f;
};

std::atomic<float> gVolume{0.8f};
std::atomic<float> gReverb{0.3f};
std::atomic<float> gEcho{0.2f};

class MicEngine : public oboe::AudioStreamDataCallback,
                  public std::enable_shared_from_this<MicEngine> {
public:
    bool start() {
        stop();
        inBuf.assign(kMaxFrames, 0.0f);

        oboe::AudioStreamBuilder outBuilder;
        outBuilder.setDirection(oboe::Direction::Output);
        outBuilder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
        outBuilder.setSharingMode(oboe::SharingMode::Exclusive);
        outBuilder.setFormat(oboe::AudioFormat::Float);
        outBuilder.setChannelCount(oboe::ChannelCount::Stereo);
        outBuilder.setUsage(oboe::Usage::Media);
        outBuilder.setDataCallback(shared_from_this());
        if (outBuilder.openStream(mOut) != oboe::Result::OK) { mOut.reset(); return false; }

        const int sr = mOut->getSampleRate();
        initEffects(sr);

        oboe::AudioStreamBuilder inBuilder;
        inBuilder.setDirection(oboe::Direction::Input);
        inBuilder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
        inBuilder.setSharingMode(oboe::SharingMode::Exclusive);
        inBuilder.setFormat(oboe::AudioFormat::Float);
        inBuilder.setChannelCount(oboe::ChannelCount::Mono);
        inBuilder.setSampleRate(sr);
        inBuilder.setInputPreset(oboe::InputPreset::VoiceRecognition);
        if (inBuilder.openStream(mIn) != oboe::Result::OK) {
            mIn.reset(); mOut->close(); mOut.reset();
            return false;
        }

        // Una ang input, para may binabasa na ang output callback
        if (mIn->requestStart() != oboe::Result::OK || mOut->requestStart() != oboe::Result::OK) {
            stop();
            return false;
        }
        return true;
    }

    void stop() {
        if (mOut) { mOut->stop(); mOut->close(); mOut.reset(); }
        if (mIn) { mIn->stop(); mIn->close(); mIn.reset(); }
    }

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream*, void* audioData, int32_t numFrames) override {
        float* out = static_cast<float*>(audioData);
        if (!mIn || numFrames > (int32_t)inBuf.size()) {
            std::memset(out, 0, sizeof(float) * 2 * (size_t)numFrames);
            return oboe::DataCallbackResult::Continue;
        }

        int32_t got = 0;
        auto r = mIn->read(inBuf.data(), numFrames, 0);
        if (r.error() == oboe::Result::OK) got = r.value();
        for (int32_t i = got; i < numFrames; ++i) inBuf[i] = 0.0f;

        const float vol = gVolume.load(std::memory_order_relaxed);
        const float rev = gReverb.load(std::memory_order_relaxed) * 1.5f;
        const float ech = gEcho.load(std::memory_order_relaxed);

        for (int32_t i = 0; i < numFrames; ++i) {
            const float in = inBuf[i] * vol;

            float sum = 0.0f;
            const float x = in * 0.03f;
            for (int c = 0; c < 4; ++c) sum += combs[c].process(x);
            sum = aps[0].process(sum);
            sum = aps[1].process(sum);

            float s = in + sum * rev + echo.process(in) * ech;
            if (s > 1.0f) s = 1.0f; else if (s < -1.0f) s = -1.0f;
            out[2 * i] = s;
            out[2 * i + 1] = s;
        }
        return oboe::DataCallbackResult::Continue;
    }

private:
    static constexpr int32_t kMaxFrames = 8192;

    void initEffects(int sr) {
        static const int kComb[4] = {1116, 1188, 1277, 1356};
        const float scale = (float)sr / 44100.0f;
        for (int i = 0; i < 4; ++i) combs[i].init((int)(kComb[i] * scale), 0.84f, 0.2f);
        aps[0].init((int)(556 * scale));
        aps[1].init((int)(441 * scale));
        echo.init((int)(0.30f * (float)sr), 0.4f);
    }

    std::shared_ptr<oboe::AudioStream> mOut;
    std::shared_ptr<oboe::AudioStream> mIn;
    std::vector<float> inBuf;
    Comb combs[4];
    Allpass aps[2];
    Echo echo;
};

std::shared_ptr<MicEngine> gMic;

}  // namespace

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_topz_karaoke_audio_MicEngine_nativeStart(JNIEnv*, jobject) {
    if (!gMic) gMic = std::make_shared<MicEngine>();
    return gMic->start() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_topz_karaoke_audio_MicEngine_nativeStop(JNIEnv*, jobject) {
    if (gMic) gMic->stop();
}

JNIEXPORT void JNICALL
Java_com_topz_karaoke_audio_MicEngine_nativeSetParams(JNIEnv*, jobject, jfloat volume, jfloat reverb, jfloat echo) {
    gVolume.store(volume);
    gReverb.store(reverb);
    gEcho.store(echo);
}

}  // extern "C"
