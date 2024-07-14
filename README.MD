# MediaPlayer

## A decode library for Minecraft based on [ffmpeg](https://ffmpeg.org/).

### Features:

* Video and audio decoder
* Hardware acceleration
* Supports many media formats
* Only Windows x64 platform is supported

### How to use:

#### For user

Use the commands

```
/summon mediaplayer:video
/mediaplayer video
```

to play video.<br><br>
Or use

```
/mediaplayer audio
```

to play audio.<br><br>
Use

```
/mediaplayer clear
```

stop all video and audio.

#### For developer

Create decoder

```java
var file = new File("<video file path>");
var decoder = VideoDecoder.create(file);
```

Decode and render frame

```java
decoder.fetch();
bufferSource.getBuffer(VideoRenderType.create(decoder.frame));
/* Rendering code */
```

Play companion audio from video

```java
Minecraft.getInstance().getSoundManager().play(new AudioInstance(decoder.audio.decode(false),null));
```

Play independent audio

```java
Minecraft.getInstance().getSoundManager().play(new AudioInstance(new AudioDecoder.decode(false),null));
```