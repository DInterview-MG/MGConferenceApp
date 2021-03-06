# MG Conference App

Simple 1:1 video conferencing app for Android using mediasoup and WebRTC.

## Features

* At startup, prompts the user for the hostname and port of the signaling server, and allows them
  to join the room.

* Captures and sends a video stream from the front webcam, and an audio stream from the microphone.

* Plays incoming video and audio streams of your choice -- this can be changed at runtime.

<p align="center"><img src="screenshot.png" alt="drawing" width="240"/></p>

## How to use

The app can be built with Gradle in the usual way:

```shell
./gradlew packageDebug

adb -s <device> install ./app/build/outputs/apk/debug/app-debug.apk
```

After installing, specify the hostname and port in the text fields, and tap "Connect via HTTPS".

You should see two dropdown lists -- these let you choose which video and audio streams to
subscribe to.

## General notes

* As discussed via email, the mediasoup Android wrapper library is unfortunately out of date,
  and is no longer available from the defunct JCenter repo. I've configured Gradle to download this
  from the JitPack repo.

* I've configured OkHttp to accept any SSL certificate, to ensure that it can connect to servers
  running a self-signed certificate. It probably goes without saying that I wouldn't do this in
  production :)

* Due to the number of callbacks coming from various threads, I've added a class called
  `SingleThreadedVar`, which contains a value, and will throw an exception if it's accessed from
  any thread other than the one it was created on. I think this is safer than e.g. simply using an
  `AtomicReference`, as it ensures that all operations involving those variables are serialized on 
  the same thread. `Atomic*` types and mutexes are often used in a way which is too granular to
  provide real thread safety.

## Limitations

There are various things I would've liked to do given more time:

* Currently some of the cleanup is prone to race conditions. For example, it's possible to clean
  up some components (e.g. `ActiveConsumer`) before they've finished being created. This would be
  fixable by delaying the cleanup until creation is completed (taking note of the cases where an
  error occurred), or alternatively by finding a way to interrupt the creation.

* Errors are not handled very gracefully -- in most cases, the app will simply show a toast, and
  it's necessary to restart the app to get back to a working state.

* When a connection is shut down, there's a flurry of internal Android logging about dead threads,
  and the WebRTC EglRenderer continues logging for about a minute. It would be good to investigate
  this further, although it may turn out to be a library bug. For now it seems harmless.

* Again due to the time constraints, there are no unit tests/other automated testing for this
  project. In a production app, it would be good to have automated testing of end-to-end calls.
