# # OpenKaraoke-by-Topz 🎤

Offline MIDI/KAR karaoke player para sa Android. Walang internet na kailangan,
at ikaw ang magdadala ng sarili mong mga kanta.

## Mga Feature (planned)

- [ ] MIDI/KAR playback gamit ang FluidSynth + SoundFont
- [ ] Lyrics na nagha-highlight habang tumutugtog
- [ ] Offline song library na may search (number, title, artist)
- [ ] Reservation queue
- [ ] Transpose (key change) at tempo control
- [ ] Mute ng melody/vocal channel
- [ ] Mic echo/reverb
- [ ] TV output (HDMI/Chromecast)

## Tech Stack

- Kotlin + Jetpack Compose
- FluidSynth (NDK/JNI) para sa MIDI synthesis
- Oboe para sa low-latency audio
- Room para sa song database

## Mahalagang Paalala

Ang OpenKaraoke ay **player lang**. Hindi ito kasama ng anumang copyrighted
na kanta. I-import mo ang sarili mong mga MIDI/KAR files. Huwag mag-upload
ng copyrighted na kanta sa repo na ito.

## Paano i-build

_Susunod._

## Roadmap

1. MVP: load at play ng isang .mid file na may lyrics
2. Library at search
3. Karaoke controls (transpose, tempo, mute, queue)
4. Polish (backgrounds, mic echo, TV output)

## License

Apache-2.0 (para sa code lamang, hindi sa mga kanta o SoundFont).
