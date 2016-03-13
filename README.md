# JustBeatIt

This app is a project for the lecture "Systemtheorie 2" at the Ruhr University Bochum.

**Usage**

Just open the app and your flash should immediately be turned on. Then just place (preferably) your index findex on the camera lens. The preview in the bottom left corner will help you to adjust the pressure of your finger. Avoid to high average red pixel values (close to 240) or the readings are not reliable anymore. It even works great with almost no light (avg. 50). You can also pause and resume the monitoring anytime by just pressing the button in the bottom right corner.

**How it works**

The app grabs the latest frame of the camera preview, decodes the YUV420 data format and counts the average amount of red pixels in the given frame. It stores the values and after some integer smoothing of the data a heart beat is registered, when the average amount of red pixels is higher than the stored average. 

For convenience the registered beats are shown in a graph, whose speed can be configured in the settings. There you can also adjust the recognition threshold, if your flash is not directly adjacent to your camera, which allows to get a good reading even if the flash only barely shines through your finger.
