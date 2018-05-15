Hi!

It appears you have downloaded IldaViewer. How brave of you. Well, I'll assume this was entirely your own choice. 



Prerequisites:

-A JRE (Java Runtime Environment) that is sufficiently up to date
-A GPU which supports OpenGL 2.0 or higher (if not, try updating your video card drivers)

IldaViewer is pretty badly written so make sure you have enough RAM.



Important:

IldaViewer is intended to view/create/export laser art. You should be aware that lasers are dangerous when not used appropriately. The author of this program refuses any responsability for damage resulting in the use of artwork created with this program. Always observe your local regulations and please, don't audience scan if you don't know what you're doing. Even if you think you know what you're doing, reconsider.



How to run:

On Windows 64 bit, double click IldaViewer.exe. Make sure you have Java installed.

If the program fails somehow, like you only get a black screen or nothing at all, try double clicking IldaViewer.bat. If that file isn't included in the install, create a new text file called IldaViewer.bat and with the following text:

@echo off
java -Djava.ext.dirs=lib -Djava.library.path=lib IldaViewer

Make sure it's in the same folder as IldaViewer.exe and the lib folder. Upon double clicking this file you should get a console window where hopefully no error messages appear. If error messages do appear however, you can send them to me. The most frequent error message is related to OpenGL not being available on your system. In this case you can upgrade your graphics card. Look at your graphic's card manufacturers website. Make sure you use the correct graphics card if you have multiple. Make sure you have done this before bugging me. Another possible problem is that you don't have java correctly installed.


On OSX, Windows 32 bit or Linux, you'll need to obtain a compiled version for your OS. If you can't find it, you'll need to compile it yourself, which isn't hard. It sounds scarier than it is, I promise. See the How to compile.txt file.



How to use:

Several options can be changed in the IldaViewer.ini file, which is inside the data folder. The most important one is probably the frame size. Due to a bad programming choice, IldaViewer's screen always needs to be square, but there's no reason it should be limited to 600 pixels. Rich guys with lots of pixels on their screen can go crazy here. Also things like the default palette or the GUI font can be changed.

IldaViewer has a number of tabs, each with their own capabilities.

ILDAVIEWER TAB

The first tab is the main IldaViewer tab. Upon launching, you get a button to load an ilda file. Clicking this opens a file dialog where you can select an ilda file. You can also drag and drop ilda files into the program window (this is the only way to open multiple ilda files at once). If the import is successful, additional buttons appear. A button removes all frames from the program, another toggles the display of blanked lines. If the imported ilda file is a palette file (format 0 or 1), two buttons appear to choose another palette, if more than one is loaded into the program. If more than one frame is loaded, a player is visible where you can toggle automatic playback of files.
Dragging the mouse around rotates the frame. Using the scroll wheel or the right mouse button zooms. Double clicking resets the camera.


SEQUENCE EDITOR TAB

The second tab has a list of all loaded frames. There are two columns, which are only visible if frames are in them. The Main column has all the frames that are loaded in the program and can be retrieved in other tabs. The Buffer column only exists in the Sequence editor tab and acts as a temporary storage when eg. reordering the order in which the frames exist or when saving a couple frames while deleting the rest. 

The currently displayed frame in the background has a yellow border. By default, this is also the selected frame which gets deleted when you click the delete frame button or the delete key on the keyboard. You can multiselect frames by right clicking. Holding shift selects all frames from the yellow to the current frame. This way operations can be done easily over a large amount of frames.

Frames can be dragged and dropped from one list to another and inserted in between. New, empty frames can be added by dragging from the New frame button to a frame list. Frames dragged to the delete frame button get deleted. Frames dropped elsewhere return to their original position. Frames loaded by clicking the load ilda file button can get dragged from the area below this button to the desired location.

There are four options you can select that happen when you drop frames over a list: insert, which inserts frames in between the other frames (indicated by the other frames moving downward); overwrite which overwrites the frames which come after the frame location (indicated by red); merge which merges all points from the dropped frames to the frames already in place (indicated by green) and copy frame data which allows you to select which properties of the dropped frame get written to the existing frames (for example you can write only the colour of a frame to another), this is indicated in blue.

Frames can get copied by holding the control key while dragging.


FRAME EDITOR TAB

wip. Don't look here. Avert your eyes. Nothing to see. Move along please.
(it's invisible now. because there was nothing to see. Don't go looking for it, you've been warned)


PALETTE EDITOR TAB

Format 0 and 1 files use palettes to store their colours. The palette in this tab is always the default palette which is used when loading in a file, so if you import a file and find out the program used the wrong palette, make sure the correct palette is active before trying again. If you're not so patient, you can click the Recolour all palette frames button once you loaded in the correct palette, which changes all palette frames' colours to the ones from the currently active palette. You can change which palette is active by clicking in the list under All palettes. You can click on a colour and change its RGB values using the sliders at the bottom. You can also change the palette name or the number of colours by typing the appropriate value into the textbox and hit enter. When you resize a palette to an amount of colours greater than the existing one, you get random colours. 

When creating a new palette, it has 0 colours so you need to enter the desired amount of colours for it to be of any use.

Palettes can get exported as image files (jpg, png or bmp). This creates a one pixel high image with a width equal to the amount of colours which you can edit in a separate image file editor. This way you can easily generate or edit palettes by using such a program's colour gradient or image transform options. 


OSCILLABSTRACT TAB

Hey look at that. A fully featured node based ilda file editor. For free. Yup.

By default, you get a workspace with two elements, a Source and an Output, connected together with a blue connection. A Source provides a stream of ilda frames, either some generic frames such as a circle, line, triangle, ... or a selection of frames from the main program's frame list. The blue connection signifies that the data passed along is a frame. The output element accepts this frames in its blue input node and stores them. Only one frame is stored, unless you provide input to its Sync input (which is the position in the internal storage at which the frames get saved) or you click the Record button (which saves frame after frame in its internal storage). 

An Oscillabstract workspace should always have an Output element. When you click Import in IldaViewer, the frames stored in the Output element get transferred to the main program's frame list. 

Apart from frames, you can also transfer data values from one element to another. This is signified by a white connection.

To create a connection, click on a node and drag to another node. When you fail, the connection disappears. Nodes can only exist between an input and an output node, not between two input or two output nodes. You also can't connect a frame node to a data node or vice versa. An input node can only have one connection, an output node can have as many as are required. To remove a node, click on where it's connected to an input node (on the left side) and drag it to an empty spot.

To create an element, click on an empty location on the workspace. This brings up a list with all elements. Clicking on a button will create the element.

The workspace can be dragged around with the middle mouse button or while holding control and dragging.

To reset a workspace, click Clear and then press enter (as a safeguard for accidentally deleting the workspace). This will result in an empty workspace with only an Output element. You can save a workspace and load it back in again. Oscillabstract workspaces have the .osc extension. It's a good idea to load in a few example workspaces to get used to the Oscillabstract mode. Warning: when loading in another workspace, the current workspace gets permanently deleted.

Here is a list of all elements in order:

Source - this provides a source of frames. Click select frames to bring up the source dialog. There are two rows. The first row displays all frames loaded into the program, the second some frames generated by default. To select frames from the first row, left click on the first frame you want and right click on the last frame, then hit Accept. For the second row, a simple left click suffices. When more than one frame is loaded, hitting Autoplay will toggle an automatic succession of the frames. The Sync output is the number of the currently displayed frame. With the Sync input, the currently displayed frame can be selected. A Source element can also be created by dragging an ilda frame from a file browser to an empty location on the workspace.

Oscillator - a wave generator. Click the green waveform to select another wavetype: sine, cosine, ramp, triangle, sawtooth, square, random, noise. You can change the properties of the wave by connecting values to the various inputs. 
*Input: when possible, the output will be determined by this value eg. it will take the sine of this value
*Waveform: selects the current waveform by an integer value
*Frequency: how "fast" the waveform changes, or the x-size of the waveform. Observe that for sines etc, one complete period would go from zero to two pi (6.28...) but it has been rescaled in IldaViewer so that a complete period goes from zero to one.
*Phase: the initial position of the waveform on the x axis - shifts the waveform left and right
*Amplitude: the y-size of the waveform. Observe that by default, waves are between -0.5 and 0.5 instead of -1 and 1, so that you can generate full-size frames without resizing (as the width of the window is equal to 1).
*Offset: the y position of the waveform
*Samples: the amount of data points that are provided, by default 200
Hint: colours need a value between 0 and 1 but all waveforms exist between -0.5 and 0.5 so if you hook up a constant set to 0.5 to offset, you can effortlessly do colour transforms using oscillators. XYZ transforms are already properly scaled so you can just hook up a sine and a cosine to an X and Y input and get a circle without having to change any parameters.

Translate - translates points of the Input frame in 3D space. You can input a single value (like the output from a Constant element) or multiple values (like the output of an Oscillator element)

Rotate - rotates points of the Input frame in 3D space. A value of 1 corresponds to 360° or a complete rotation. The rotation order is fixed (first rotation around the X axis, then Y and then Z) but if you need another rotation order, simply use two separate rotation events. By clicking the Pivot point button, you can specify the x, y and z coordinates of the pivot (fulcrum) point around which the rotation happens. By default this is the center of the screen.

Scale - changes the scale of the points of the Input frame. The scale can be changed as a whole (combined) or by the three coordinates separately. An anchor point can be specified, to which respect the scaling happens. By default this is the center of the screen.

Merge - merges two or more frames together. To merge more frames together, click Options and then the Add node button. Under options, you can also select which properties from the first frame need to be written to the others. If none are selected, all points are simply merged.

Segment - The first output, Total, is the total amount of segments in the input frame. A segment is a connected bunch of lit (unblanked) points. The second output assigns a number to a point corresponding to the segment it's in (between 0 and 1). This is useful for applying an effect like translation or rotation or colour to all points of one segment so all points in the segment get affected in the same way while the effect is different for points in another segment.

Clip - [TODO - BUGGED - TEMPORARILY REMOVED] Cuts off points outside of the boundaries specified by the inputs.

RGB - Change this frame's colours by hooking up inputs. You can change the intensity as well. You can change between Overwrite and Multiply mode. This last mode allows for some advanced colour effects such as saturation.

Palette - make a frame use a palette. You can select the palette under Options (by default it is the currently active palette). 

Palettifier - retrieve the best fitting colour index in a palette based on the input frame's RGB values.

Optimise - [TODO - INCOMPLETE] Prepare the frame for scanning through a scan set. Clicking settings opens a window with all parameters that can be changed. 

Breakout - get all frame data to use in other elements.

Breakin - Compose a frame by connecting data to the inputs. 

Constant - outputs a single constant value. Change the value by clicking on the number box and drag the mouse up or down. The further you go, the faster the value changes (by a cube law). When you hit the control key while dragging, the value snaps to the nearest half integer.
Clicking on the numberbox will allow you to enter a number. You can either use the , or . as a decimal sign. Hitting the up and down arrow keys shifts the value by 0.01. Page up and page down shift the value by 1 and -1.

Add/Subtract - adds or subtracts two or more values. To add more values, click the + button. To change to subtract mode, click Add so it changes to subtract. All other inputs are then subtracted from the first input.

Multiply/Divide - Multiplies or divides two or more values. To multiply more values, click the + button. To change to divide mode, click the multiply button so it changes to divide. The first input is then divided by all other inputs.

Math - several mathematical formulas with a various number of inputs. Click the arrow keys to cycle through the functions. The number of arguments/inputs (a, b, c, d) is variable, inputs which aren't used are marked with a dark red colour. The status text should give an explanation of what the function does and the mathematical formula is displayed. 

Logic - Several logical operations like and, or, not, if, smaller than, larger than, etc., similar to the math element. Inputs and outputs turn bright green for true and bright red for false.

Clock - outputs a value which goes linearly from 0 to 1 in one second. You can change the speed by hooking up a value to the Frequency input. You can change the shape by hooking up an input composed of multiple values to the Shape input (the output from an Oscillator is perfect for this). Use the Reset button to reset the clock, it resets every time an integer different from the previous input is detected.

RGB2HSB - converts RGB values to HSB values (hue, saturation, brightness). The hue value is the colour: 0 represents red, 0.25 is green, 0.65 is blue etc. Saturation is the "whiteness" of the colour: 0 is completely white (no matter the hue value) and 1 is a fully saturated colour. Brightness is the intensity. There is a button which toggles the conversion mode: HSB values are then converted to RGB. 

XYZ2Rθφ - [BUGGED - TODO - TEMPORARILY REMOVED] converts xyz coordinates to spherical coordinates (radius, theta, phi) where R is the distance from the origin (centre) to the point, theta is the angle the point makes with the horizontal XZ plane and phi the angle the point makes with the vertical XZ plane. A 2D image will thus have a phi value of 0. There is a button to swap conversion mode, so spherical coordinates are converted to xyz.
The origin button, when pressed, brings up three more inputs which are the xyz coordinates of the origin from which the conversion is calculated.

BufferShift - connect a stream of values to Input (such as the output of an oscillator, xyz coordinates or palette indices) and a value to Offset. The position of the values from Input are then shifted with the Offset value (and get placed back at the beginning of the stream when they reach the end). This allows for, for example, a palette rotation effect.

Inspect - a little tool to visualise data or frames. Details brings up more information about the data or frame.


EXPORT TO FILE TAB

In the last tab you can export the program's frames to ilda (frames which are not the main program's frames such as the ones in the buffer list in the sequence editor or in the output element in oscillabstract need to get transported to the main frame list first). There are several options: ilda format 0, 1, 4 or 5; LSX/LDS files PIC (single frame) and CAT. With formats 0 and 1, you get the option to include the palette in the file and if you do, there is an option to find the best fitting palette colour for RGB values. If you want only a part of the frames exported, deselect Export all frames, left click on the first frame you want exported in the list that appears and right click on the last one. The selected frames should now get highlighted. When the correct options are opted for, click the big export button and enjoy your steamy fresh new art in programs that aren't IldaViewer.
