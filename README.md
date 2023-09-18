# Fast-Edits
A CLI-Application which allows for easy montage editing to a given audio source. 

## How it works
After specifying a video and audio input, the audio may be analysed based on custom parameters. These parameters can be fine-tuned in a wave-form representation overlay. 
The video player allows for setting timestamps in the supplied source material. These timestamps will then be used as starting points for the detected beats in the audio.
After the beat, the next sequence is started, & the next timestamp is used as a starting point.

## Features 
* Specify your own input. Whether it's a movie, a sample video, or something entirely else.
* Specify the audio-analysis parameters.
* Inbuilt real-time audio analysis feedback in a UI.
* Video playback for granular control over the scenes.
* Hw-accelerated rendering.
* Inbuilt rendering options, such as fade-out, frame interpolation, etc.

## Possible caveats  
As this program has been developed for a very short time and is an open-sourced in-house tool, certain specifics may not work with your input.
Reading the provided output log is helpful; video codecs might have to be changed, depending on your input.
If you don't know what you are doing, I would advise you to a) wait for a more polished version or b) wait for a binary release. If you still feel frisky and the setup or something else does not work, feel free to open up an issue.


 ## Results
* ![2001: A Space Odyssey](https://www.youtube.com/watch?v=2PLIkvabFRs)
* ![No country for old men](https://www.youtube.com/watch?v=MF9RxFMAMZ4)
* ![Blade Runner 2049](https://www.youtube.com/watch?v=kDtZDusWHGQ)
* ![All Quiet On The Western Front](https://www.youtube.com/watch?v=20GEEztj7lA)
* ![Drive](https://www.youtube.com/watch?v=LnX8RPeWCMs)
* ![Bullet Train](https://www.youtube.com/watch?v=CNDXf7E-hW4)

