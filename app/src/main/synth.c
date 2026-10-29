// Manipis na JNI wrapper sa TinySoundFont (MIT). Lahat ng tawag ay galing sa iisang lock sa Kotlin
// (AudioEngine), kaya walang sariling locking dito.
#include <jni.h>
#include <string.h>

#define TSF_IMPLEMENTATION
#include "tsf.h"

#define NUM_CHANNELS 16
#define MAX_VOICES 64

static tsf* g_tsf = NULL;

static void init_channels(void) {
    int ch;
    if (!g_tsf) return;
    for (ch = 0; ch < NUM_CHANNELS; ch++) {
        // Ang channel 10 (index 9) ay drums. Kung walang preset 0, gamitin ang unang preset.
        if (!tsf_channel_set_presetnumber(g_tsf, ch, 0, ch == 9) && tsf_get_presetcount(g_tsf) > 0) {
            tsf_channel_set_presetindex(g_tsf, ch, 0);
        }
    }
}

JNIEXPORT jboolean JNICALL
Java_com_topz_karaoke_audio_NativeSynth_nativeLoad(JNIEnv* env, jobject thiz, jstring path,
                                                   jint sampleRate, jfloat gainDb) {
    const char* cpath;
    tsf* f;
    (void)thiz;
    if (g_tsf) { tsf_close(g_tsf); g_tsf = NULL; }
    cpath = (*env)->GetStringUTFChars(env, path, NULL);
    if (!cpath) return JNI_FALSE;
    f = tsf_load_filename(cpath);
    (*env)->ReleaseStringUTFChars(env, path, cpath);
    if (!f) return JNI_FALSE;
    tsf_set_output(f, TSF_STEREO_INTERLEAVED, sampleRate, gainDb);
    tsf_set_max_voices(f, MAX_VOICES);
    g_tsf = f;
    init_channels();
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_topz_karaoke_audio_NativeSynth_nativeUnload(JNIEnv* env, jobject thiz) {
    (void)env; (void)thiz;
    if (g_tsf) { tsf_close(g_tsf); g_tsf = NULL; }
}

JNIEXPORT void JNICALL
Java_com_topz_karaoke_audio_NativeSynth_nativeRender(JNIEnv* env, jobject thiz, jshortArray buffer,
                                                     jint frames) {
    jshort* out;
    (void)thiz;
    out = (*env)->GetShortArrayElements(env, buffer, NULL);
    if (!out) return;
    if (g_tsf) tsf_render_short(g_tsf, (short*)out, frames, 0);
    else memset(out, 0, (size_t)frames * 2 * sizeof(short));
    (*env)->ReleaseShortArrayElements(env, buffer, out, 0);
}

JNIEXPORT void JNICALL
Java_com_topz_karaoke_audio_NativeSynth_nativeNoteOn(JNIEnv* env, jobject thiz, jint ch, jint key, jint vel) {
    (void)env; (void)thiz;
    if (g_tsf && ch >= 0 && ch < NUM_CHANNELS && key >= 0 && key < 128)
        tsf_channel_note_on(g_tsf, ch, key, (float)vel / 127.0f);
}

JNIEXPORT void JNICALL
Java_com_topz_karaoke_audio_NativeSynth_nativeNoteOff(JNIEnv* env, jobject thiz, jint ch, jint key) {
    (void)env; (void)thiz;
    if (g_tsf && ch >= 0 && ch < NUM_CHANNELS && key >= 0 && key < 128)
        tsf_channel_note_off(g_tsf, ch, key);
}

JNIEXPORT void JNICALL
Java_com_topz_karaoke_audio_NativeSynth_nativeProgram(JNIEnv* env, jobject thiz, jint ch, jint program) {
    (void)env; (void)thiz;
    if (g_tsf && ch >= 0 && ch < NUM_CHANNELS) tsf_channel_set_presetnumber(g_tsf, ch, program, ch == 9);
}

JNIEXPORT void JNICALL
Java_com_topz_karaoke_audio_NativeSynth_nativeControl(JNIEnv* env, jobject thiz, jint ch, jint ctrl, jint value) {
    (void)env; (void)thiz;
    if (g_tsf && ch >= 0 && ch < NUM_CHANNELS) tsf_channel_midi_control(g_tsf, ch, ctrl, value);
}

JNIEXPORT void JNICALL
Java_com_topz_karaoke_audio_NativeSynth_nativePitchBend(JNIEnv* env, jobject thiz, jint ch, jint value) {
    (void)env; (void)thiz;
    if (g_tsf && ch >= 0 && ch < NUM_CHANNELS) tsf_channel_set_pitchwheel(g_tsf, ch, value);
}

JNIEXPORT void JNICALL
Java_com_topz_karaoke_audio_NativeSynth_nativeChannelNotesOff(JNIEnv* env, jobject thiz, jint ch) {
    (void)env; (void)thiz;
    if (g_tsf && ch >= 0 && ch < NUM_CHANNELS) tsf_channel_note_off_all(g_tsf, ch);
}

JNIEXPORT void JNICALL
Java_com_topz_karaoke_audio_NativeSynth_nativeReset(JNIEnv* env, jobject thiz) {
    (void)env; (void)thiz;
    if (g_tsf) { tsf_reset(g_tsf); init_channels(); }
}
