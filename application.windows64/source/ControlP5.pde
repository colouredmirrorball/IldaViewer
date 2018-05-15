/*
 * In this method all GUI elements are added.
 * For more information, see the ControlP5 documentary.
 * Callback methods for elements who have them are placed in the main tab.
 *
 * In short, you can add elements (buttons, toggles, lists, ...) to ControlP5. 
 * Each element has a name in the form of a String. In some cases, this can correspond
 * to a variable. For example, if you add a Toggle called "theToggle", and you have a
 * boolean theToggle in the program, the boolean will automatically get the value of the
 * Toggle that is being displayed on screen. (Reverse is not true, when some program element
 * changes the value of the boolean theToggle, the Toggle GUI button doesn't automatically
 * update. You need to manually set this by doing something like cp5.getController("theToggle").setValue(boolean(int(theToggle))); )
 * 
 * All GUI elements are grouped in Tabs. 
 */




void initializeControlP5()
{

  //Mouseover help delay:
  cp5.getTooltip().setDelay(500);

  //Add all ControlP5 elements:

  //      === TAB: DEFAULT ===

  cp5.getTab("default")       //No need to initiate this tab
      .activateEvent(true)
      .setLabel("IldaViewer")
        .getCaptionLabel().setFont(f10);


  cp5.addButton("LoadIldaFile")
    .setPosition(10, 25)
      .moveTo("default")
        .setSize(100, 25)
          .setMoveable(true)
            .setLabel("Load an Ilda file")
              .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.getTooltip().register("LoadIldaFile", "Load an Ilda file and add it at the end of the existing frame list").getLabel().toUpperCase(false);

  importFile = cp5.addMultiList("importFile", 10, 55, 100, 25);
  importFile.setLabel("Import");
  MultiListButton b = importFile.add("Import", 1);
  b.add("Ilda", 2);
  b.add("Pic", 3);
  b.add("Cat", 4);
  b.add("Palette image", 5);
  b.add("Frame txt", 6);
  b.add("Oscillabstract", 7);

  cp5.addButton("clearFrames")
    .setPosition(width-130, 25)
      .setSize(120, 25)
        .moveTo("default")
          .setVisible(false)
            .setMoveable(true)
              .setLabel("Clear all frames")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.getTooltip().register("clearFrames", "Remove all frames").getLabel().toUpperCase(false);

  Group framePlayer = cp5.addGroup("framePlayer") //A group, for convenience
    .setPosition(width-240, 35)
      .setVisible(false)
        .moveTo("default")
          ;

  PImage[] previmgs = {
    loadImage("Images/previousframedefault.png"), loadImage("Images/previousframeactive.png"), loadImage("Images/previousframemouseover.png")
    };

    cp5.addBang("previousFrame")
      .setPosition(20, 5)
        .setImages(previmgs)
          .updateSize()
            .setTriggerEvent(Bang.RELEASE)
              .setLabel("<")
                .setGroup(framePlayer)
                  ;

  PImage[] nextimgs = {
    loadImage("Images/nextframedefault.png"), loadImage("Images/nextframeactive.png"), loadImage("Images/nextframemouseover.png")
    };

    cp5.addBang("nextFrame")
      .setPosition(60, 5)
        .setImages(nextimgs)
          .updateSize()
            .setGroup(framePlayer)
              .setTriggerEvent(Bang.RELEASE)
                .setLabel(">")
                  ;

  PImage[] firstimgs = {
    loadImage("Images/firstframedefault.png"), loadImage("Images/firstframeactive.png"), loadImage("Images/firstframemouseover.png")
    };

    cp5.addBang("firstFrame")
      .setPosition(0, 5)
        .setImages(firstimgs)
          .updateSize()
            .setGroup(framePlayer)
              .setTriggerEvent(Bang.RELEASE)
                .setLabel("<<")
                  ;

  PImage[] lastimgs = {
    loadImage("Images/lastframedefault.png"), loadImage("Images/lastframeactive.png"), loadImage("Images/lastframemouseover.png")
    };

    cp5.addBang("lastFrame")
      .setPosition(80, 5)
        .setImages(lastimgs)
          .updateSize()
            .setGroup(framePlayer)
              .setTriggerEvent(Bang.RELEASE)
                .setLabel(">>")
                  ;

  PImage[] playimgs = {
    loadImage("Images/playdefault.png"), loadImage("Images/playmouseover.png"), loadImage("Images/playactive.png")
    };

    cp5.addButton("autoPlay")      //This is supposed to be a Toggle, but images won't work for some reason
        .setPosition(40, 5)
        .setImages(playimgs)
          .setSwitch(true)
            .setGroup(framePlayer)
              .updateSize()
                .setLabel("Autoplay");



  cp5.addSlider("playbackSpeed")    //There is no callback method, as playback speed is stored in the variable playbackSpeed
    .setPosition(00, 30)
      .setSize(100, 20)
        .setRange(-60, 60)
          .setValue(12)
            .setGroup(framePlayer)
              .setValueLabel("Playback speed")
                ;

  cp5.getController("playbackSpeed").getCaptionLabel().setVisible(false);


  cp5.addBang("previousPalette")
    .setPosition(10, 85)
      .setSize(30, 30)
        .moveTo("default")
          .setTriggerEvent(Bang.RELEASE)
            .setLabel("< pal")
              .setVisible(false)
                ;

  cp5.getTooltip().register("previousPalette", "Recolour frame with previous palette in the palette list").getLabel().toUpperCase(false);

  cp5.addBang("nextPalette")
    .setPosition(42, 85)
      .setSize(30, 30)
        .moveTo("default")
          .setTriggerEvent(Bang.RELEASE)
            .setLabel("ette >")
              .setVisible(false)
                ;

  cp5.getTooltip().register("nextPalette", "Recolour frame with next palette in the palette list").getLabel().toUpperCase(false);

  cp5.addToggle("showBlanking")
    .setPosition(width-130, 60)
      .setSize(120, 25)
        .moveTo("default")
          .setVisible(false)
            .setMoveable(true)
              .setValueLabel("Show blanking lines")
                .setCaptionLabel("Show blanking")
                  .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.getTooltip().register("showBlanking", "Toggle the display of blanked lines").getLabel().toUpperCase(false);

  cp5.addToggle("showHeader")
    .setPosition(width-130, 106)
      .setSize(120, 12)
        .moveTo("default")
          .setVisible(true)
            .setMoveable(true)
              .setValueLabel("Show information")
                .setCaptionLabel("Show information")
                  .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.getTooltip().register("showHeader", "Show additional information about the program and frame").getLabel().toUpperCase(false);

  cp5.addToggle("laserboyMode")
    .setPosition(width-130, 90)
      .setSize(120, 12)
        .moveTo("default")
          .setValue(laserboyMode)
            .setVisible(true)
              .setMoveable(true)
                .setValueLabel("Show information")
                  .setCaptionLabel("Laserboy mode")
                    .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.getTooltip().register("laserboyMode", "And unicorns. Rainbows and unicorns.").getLabel().toUpperCase(false);

  //      === TAB: SEQUENCE EDITOR ===

  cp5.addTab("seqeditor")
    .activateEvent(true)
      .setLabel("Sequence Editor")
        .setId(1);



  cp5.addBang("insertFrame")
    .setPosition(width-300, 30)
      .setSize(85, 40)
        .moveTo("seqeditor")
          .setVisible(true)          
            .setMoveable(true)
              .setLabel("New frame")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addTextfield("nrOfInsertedFrames")
    .setPosition(width-300, 75)
      .setSize(85, 20)
        .setFocus(true)
          .setText(nrOfInsertedFrames)
            .moveTo("seqeditor")
              .setVisible(true)
                .setMoveable(true)
                  .setLabel("Amount")
                    .setAutoClear(false);


  cp5.addButton("deleteFrame")
    .setPosition(width-300, 125)
      .setSize(85, 85)
        .moveTo("seqeditor")
          .setVisible(true)
            .setMoveable(true)
              .setLabel("Delete frame")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addToggle("graphicView")
    .setPosition(width-300, 230)
      .setSize(85, 40)
        .moveTo("seqeditor")
          .setVisible(true)
            .setMoveable(true)
              .setLabel("Graphic view")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("editorLoadIldaFile")
    .setPosition(width-300, 380)
      .moveTo("seqeditor")
        .setSize(85, 40)
          .setMoveable(true)
            .setLabel("Load Ilda file")
              .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("revrs")
    .setPosition(width-300, 290)
      .moveTo("seqeditor")
        .setSize(85, 40)
          .setMoveable(true)
            .setLabel("Reverse")
              .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("randomize")
    .setPosition(width-300, 335)
      .moveTo("seqeditor")
        .setSize(85, 40)
          .setMoveable(true)
            .setLabel("Randomize")
              .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("editorClearFrames")
    .setPosition(width-300, 440)
      .setSize(85, 40)
        .moveTo("seqeditor")
          .setVisible(true)
            .setMoveable(true)
              .setLabel("Clear")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("editorClearBuffer")
    .setPosition(width-300, 510)
      .setSize(85, 40)
        .moveTo("seqeditor")
          .setVisible(true)
            .setMoveable(true)
              .setLabel("Clear")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("editorFitPalette")
    .setPosition(width-190, 30)
      .setSize(120, 40)
        .moveTo("seqeditor")
          .setVisible(true)
            .setMoveable(true)
              .setLabel("Fit RGB to palette")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);


  copyBehaviour = cp5.addRadioButton("copyBehaviour")
    .setPosition(width-190, 85)
      .moveTo("seqeditor")
        .setSize(25, 25)
          .setItemsPerRow(1)
            .setSpacingColumn(5)
              .setSpacingRow(4)
                .setNoneSelectedAllowed(false)
                  .addItem("Insert", 0)
                    .addItem("Overwrite", 1)
                      .addItem("Merge", 2)
                        .addItem("Copy frame data", 3)
                          .activate(0);


  copiedElements = cp5.addCheckBox("copiedElements")
    .setPosition(width-190, 210)
      .setSize(20, 20)
        .moveTo("seqeditor")
          .setVisible(false)
            .setItemsPerRow(2)
              .setSpacingColumn(75)
                .setSpacingRow(4)
                  .addItem("Frame header", 1)
                    .addItem("Point number", 1)
                      .addItem("X", 1)
                        .addItem("R", 1)
                          .addItem("Y", 1)
                            .addItem("G", 1)
                              .addItem("Z", 1)
                                .addItem("B", 1)
                                  .addItem("Blanking", 1)
                                    .addItem("Palette index", 1);

  cp5.addButton("editorSelectFrames")
    .setPosition(width-200, 440)
      .setSize(85, 40)
        .moveTo("seqeditor")
          .setVisible(true)
            .setMoveable(true)
              .setLabel("Select all")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("editorSelectBuffer")
    .setPosition(width-200, 510)
      .setSize(85, 40)
        .moveTo("seqeditor")
          .setVisible(true)
            .setMoveable(true)
              .setLabel("Select all")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("editorDeselectFrames")
    .setPosition(width-100, 440)
      .setSize(85, 40)
        .moveTo("seqeditor")
          .setVisible(true)
            .setMoveable(true)
              .setLabel("Deselect all")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("editorDeselectBuffer")
    .setPosition(width-100, 510)
      .setSize(85, 40)
        .moveTo("seqeditor")
          .setVisible(true)
            .setMoveable(true)
              .setLabel("Deselect all")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);


  /*
  cp5.addToggle("editHeader")
   .setPosition(width-90, 88)
   .setSize(85, 10)
   .moveTo("seqeditor")
   .setVisible(true)
   .setMoveable(true)
   .setValueLabel("Edit")
   .setCaptionLabel("Edit")
   .getCaptionLabel().alignY(CENTER).alignX(CENTER);
   */

  //      === TAB: FRAME EDITOR ===

  /*
  cp5.addTab("framed")
   .activateEvent(true)
   .setLabel("Frame Editor")
   .setId(2);
   
   Group visibleControls = cp5.addGroup("visiblecontrols")
   .setPosition(10, 35)
   .setSize(120, 20)
   .setVisible(true)
   .setLabel("Control panels")
   .moveTo("framed");
   
   //This checkbox determines which GUI groups are visible
   //You need to program this in the setFrameEditorControls() method in the FrameEditor tab!
   
   ctrlsVis = new int[6];  //Total number of control groups
   
   controlsVisible = cp5.addCheckBox("visibleControls")
   .setPosition(0, 5)
   .setSize(15, 15)
   .setGroup(visibleControls)
   .setMoveable(true)
   .setItemsPerRow(1)
   .setSpacingColumn(17)
   .setSpacingRow(2)
   .addItem("Frame picker", 1)
   .addItem("Raster image", 10)
   .addItem("View Rotation", 100)
   .addItem("View Scale", 1000)
   .addItem("View position", 10000)
   .addItem("View Perspective", 100000);
   
   controlsVisible.activate(0);
   
   
   
   Group framePicker = cp5.addGroup("framePicker") //A group, for convenience
   .setPosition(150, 35)
   .setSize(120, 20)
   .setVisible(false)
   .moveTo("framed")
   ;
   
   cp5.addBang("previousFrameEd")
   .setPosition(20, 5)
   .setImages(previmgs)
   .updateSize()
   .setTriggerEvent(Bang.RELEASE)
   .setLabel("<")
   .setGroup(framePicker)
   ;
   
   cp5.addBang("nextFrameEd")
   .setPosition(80, 5)
   .setImages(nextimgs)
   .updateSize()
   .setGroup(framePicker)
   .setTriggerEvent(Bang.RELEASE)
   .setLabel(">")
   ;
   
   cp5.addBang("firstFrameEd")
   .setPosition(0, 5)
   .setImages(firstimgs)
   .updateSize()
   .setGroup(framePicker)
   .setTriggerEvent(Bang.RELEASE)
   .setLabel("<<")
   ;
   
   cp5.addBang("lastFrameEd")
   .setPosition(100, 5)
   .setImages(lastimgs)
   .updateSize()
   .setGroup(framePicker)
   .setTriggerEvent(Bang.RELEASE)
   .setLabel(">>")
   ;
   
   cp5.addToggle("listFrameEd")
   .setPosition(40, 5)
   .setSize(37, 20)
   .setGroup(framePicker)
   .setCaptionLabel("List")
   .getCaptionLabel().alignX(CENTER).alignY(CENTER)
   ;
   
   
   
   Group rasterImage = cp5.addGroup("rasterImage") //A group, for convenience
   .setPosition(300, 35)
   .setSize(120, 20)
   .setVisible(false)
   .setLabel("Raster image")
   .moveTo("framed")
   ;
   
   cp5.addButton("openFramedImage")
   .setPosition(0, 5)
   .setSize(57, 20)
   .setGroup(rasterImage)
   .setCaptionLabel("Open")
   .getCaptionLabel().alignX(CENTER);
   
   cp5.addToggle("hideFramedImage")
   .setPosition(63, 5)
   .setSize(57, 20)
   .setGroup(rasterImage)
   .setCaptionLabel("Hide")
   .getCaptionLabel().alignX(CENTER).alignY(CENTER);
   
   cp5.addButton("recolourFramedImage")
   .setPosition(0, 30)
   .setSize(120, 20)
   .setGroup(rasterImage)
   .setCaptionLabel("Colour to points")
   .getCaptionLabel().alignX(CENTER);
   
   cp5.addButton("positionFramedImage")
   .setPosition(0, 55)
   .setSize(57, 20)
   .setGroup(rasterImage)
   .setCaptionLabel("Position")
   .getCaptionLabel().alignX(CENTER);
   
   cp5.addToggle("sizeFramedImage")
   .setPosition(63, 55)
   .setSize(57, 20)
   .setGroup(rasterImage)
   .setCaptionLabel("Size")
   .getCaptionLabel().alignX(CENTER).alignY(CENTER);
   
   cp5.addSlider("sizeXYFramedImage")
   .setPosition(0, 80)
   .setSize(120, 20)
   .setGroup(rasterImage)
   .setRange(-1.5, 1.5)
   .setValue(1)
   .setVisible(false)
   .setSliderMode(Slider.FLEXIBLE)
   .setCaptionLabel("Size")
   .getCaptionLabel().alignX(CENTER)
   ;
   
   
   cp5.addSlider("sizeXFramedImage")
   .setPosition(0, 105)
   .setSize(120, 20)
   .setGroup(rasterImage)
   .setRange(-1.5, 1.5)
   .setValue(1)
   .setVisible(false)
   .setSliderMode(Slider.FLEXIBLE)
   .setCaptionLabel("Size X")
   .getCaptionLabel().alignX(CENTER)
   ;
   
   cp5.addSlider("sizeYFramedImage")
   .setPosition(0, 130)
   .setSize(120, 20)
   .setGroup(rasterImage)
   .setRange(-1.5, 1.5)
   .setValue(1)
   .setVisible(false)
   .setSliderMode(Slider.FLEXIBLE)
   .setCaptionLabel("Size Y")
   .getCaptionLabel().alignX(CENTER)
   ;
   
   
   
   
   Group rotationControl = cp5.addGroup("rotationControl")
   .setPosition(450, 35)
   .setSize(120, 20)
   .setVisible(false)
   .setLabel("View Rotation")
   .moveTo("framed")
   ;
   
   cp5.addSlider("rotationX")
   .setPosition(0, 5)
   .setSize(120, 20)
   .setGroup(rotationControl)
   .setRange(-1, 1)
   .setSliderMode(Slider.FLEXIBLE)
   .setCaptionLabel("X")
   .getCaptionLabel().alignX(CENTER)
   ;
   
   cp5.addSlider("rotationY")
   .setPosition(0, 30)
   .setSize(120, 20)
   .setGroup(rotationControl)
   .setRange(-1, 1)
   .setSliderMode(Slider.FLEXIBLE)
   .setCaptionLabel("Y")
   .getCaptionLabel().alignX(CENTER)
   ;
   
   cp5.addSlider("rotationZ")
   .setPosition(0, 55)
   .setSize(120, 20)
   .setGroup(rotationControl)
   .setRange(-1, 1)
   .setSliderMode(Slider.FLEXIBLE)
   .setCaptionLabel("Z")
   .getCaptionLabel().alignX(CENTER)
   ;
   
   Group sizeControl = cp5.addGroup("sizeControl")
   .setPosition(1, 1)
   .setSize(120, 20)
   .setVisible(false)
   .setLabel("View Scale")
   .moveTo("framed")
   ;
   
   cp5.addSlider("zoom")
   .setPosition(0, 5)
   .setSize(120, 20)
   .setGroup(sizeControl)
   .setRange(-0.1, 0.1)
   .setValue(0)
   .setVisible(true)
   .setSliderMode(Slider.FLEXIBLE)
   .setCaptionLabel("Zoom")
   .getCaptionLabel().alignX(CENTER)
   ;
   
   
   cp5.addButton("resetZoom")
   .setPosition(0, 30)
   .setSize(120, 20)
   .setGroup(sizeControl)
   .setCaptionLabel("Reset")
   .getCaptionLabel().alignX(CENTER);
   
   Group positionControl = cp5.addGroup("positionControl")
   .setPosition(1, 1)
   .setSize(120, 20)
   .setVisible(false)
   .setLabel("View Position")
   .moveTo("framed")
   ;
   
   Group perspective = cp5.addGroup("perspective")
   .setPosition(1, 1)
   .setSize(120, 20)
   .setVisible(false)
   .setLabel("View Perspective")
   .moveTo("framed")
   ;
   
   cp5.addSlider("viewPerspective")
   .setPosition(0, 5)
   .setSize(120, 20)
   .setGroup(perspective)
   .setRange(0, 10)
   .setValue(0)
   .setVisible(true)
   .setSliderMode(Slider.FLEXIBLE)
   .setCaptionLabel("Perspective")
   .getCaptionLabel().alignX(CENTER)
   ;
   
   */

  //      === TAB: PALETTES ===

  cp5.addTab("palettes")
    .activateEvent(true)
      .setLabel("Palette editor")
        .setId(4);

  paletteList = cp5.addListBox("myList")
    .setPosition(10, 150)
      .setSize(145, 300)
        .moveTo("palettes")
          .setLabel("All palettes")
            .setItemHeight(15)
              .setBarHeight(15)
                ;

  paletteList.captionLabel().toUpperCase(true);
  paletteList.captionLabel().style().marginTop = 3;
  paletteList.toUpperCase(false);
  paletteList.valueLabel().style().marginTop = 3;

  cp = cp5.addColorPicker("picker")
    .setPosition(width-425, 450)
      .moveTo("palettes")
        ;

  cp5.addButton("recolourFrames")
    .setPosition(10, 25)
      .setSize(145, 25)
        .moveTo("palettes")
          .setCaptionLabel(" Recolour frames")
            .setVisible(true)
              .getCaptionLabel().alignX(CENTER);


  cp5.addButton("addPalette")
    .setPosition(10, 60)
      .setSize(70, 25)
        .moveTo("palettes")
          .setCaptionLabel("New")
            .setVisible(true)
              .getCaptionLabel().alignX(CENTER);

  cp5.addButton("removePalette")
    .setPosition(85, 60)
      .setSize(70, 25)
        .moveTo("palettes")
          .setCaptionLabel("Delete")
            .setVisible(true)
              .getCaptionLabel().alignX(CENTER);

  cp5.addButton("importPalette")
    .setPosition(10, 90)
      .setSize(70, 25)
        .moveTo("palettes")
          .setCaptionLabel("Import")
            .setVisible(true)
              .getCaptionLabel().alignX(CENTER);

  cp5.addButton("exportPalette")
    .setPosition(85, 90)
      .setSize(70, 25)
        .moveTo("palettes")
          .setCaptionLabel("Export")
            .setVisible(true)
              .getCaptionLabel().alignX(CENTER);

  PFont font = createFont("arial", 12);

  cp5.addTextfield("input")
    .setPosition(width-150, 490)
      .setSize(120, 20)
        .setFont(font)
          .moveTo("palettes")
            .setCaptionLabel("Rename")
              .setFocus(true)
                // .setColor(color(255, 0, 0))
                ;

  cp5.addTextfield("numberOfColours")
    .setPosition(width-425, 515)
      .setSize(120, 20)
        .setFont(font)
          .moveTo("palettes")
            .setCaptionLabel("Number of colours (< 256)")
              .setFocus(true)
                // .setColor(color(255, 0, 0))
                ;




  //      === TAB: SEQUENCE CREATOR ===



  cp5.addTab("deluxe")        //Initiating the tab to go to the Deluxe Paint mode
    .activateEvent(true)
      .setLabel("Oscillabstract")
        .setId(5)
          //.setColorBackground(color(0, 0, 100))
          //  .setColorLabel(color(255))
          //    .setColorActive(color(0, 150, 255))
          ;
  /*
  Group seqControlsVisible = cp5.addGroup("seqControlsVisible")
   .setPosition(10, 65)
   .setSize(150, 10)
   .setVisible(true)
   .setLabel("Select a sequence mode")
   .moveTo("deluxe");
   
   seqMode = cp5.addRadioButton("seqMode")
   .setGroup(seqControlsVisible)
   .setSize(150, 25)
   .setPosition(0, 1)
   .setItemsPerRow(1)
   .setSpacingRow(1)
   .addItem("Deluxe Paint", 0)
   .addItem("Oscillabstract", 1);
   
   for (Toggle t : seqMode.getItems ())
   {
   t.getCaptionLabel().alignY(CENTER).alignX(CENTER);
   }
   */
  cp5.addButton("clearSeqCreatorFrames")
    .setPosition(width-90, 25)
      .setSize(85, 25)
        .moveTo("deluxe")
          .setMoveable(true)
            .setLabel("Clear")
              .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.getTooltip().register("clearDeluxeFrames", "Reset the current sequence").getLabel().toUpperCase(false);

  cp5.addButton("finishSeqCreator")
    .setPosition(10, 25)
      .setSize(150, 25)
        .moveTo("deluxe")      //Only visible in Deluxe Paint tab
          .setMoveable(true)
            .setLabel("Import in IldaViewer")
              .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.getTooltip().register("finishDeluxePaint", "Import sequence to frame list").getLabel().toUpperCase(false);

  cp5.addToggle("showSQBlanking")
    .setPosition(width-90, 55)
      .setSize(85, 25)
        .moveTo("deluxe")
          .setValue(showBlanking)
            .setMoveable(true)
              .setValueLabel("Show blanking lines")
                .setCaptionLabel("Show blanking")
                  .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("emptyOscElements")
    .setPosition(width-90, 25)
      .setSize(85, 25)
        .moveTo("deluxe")
          .setMoveable(true)
            .setVisible(false)
              .setLabel("Clear")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("loadElements")
    .setPosition(width-90, 55)
      .setSize(85, 25)
        .moveTo("deluxe")
          .setMoveable(true)
            .setVisible(false)
              .setLabel("Load")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("saveElements")
    .setPosition(width-90, 85)
      .setSize(85, 25)
        .moveTo("deluxe")
          .setMoveable(true)
            .setVisible(false)
              .setLabel("Save")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);



  //      === TAB: EXPORT ===

  cp5.addTab("export")
    .activateEvent(true)
      .setLabel("Export to file")
        .setId(4);

  cp5.addButton("exportFile")
    .setPosition(10, 25)
      .setSize(100, 100)
        .moveTo("export")      //Only visible in Deluxe Paint tab
          .setMoveable(true)
            .setLabel("Export file")
              .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  r = cp5.addRadioButton("radioButton")
    .setPosition(135, 25)
      .moveTo("export")
        .setSize(22, 22)
          .setItemsPerRow(1)
            .setSpacingColumn(5)
              .setSpacingRow(4)
                .setNoneSelectedAllowed(false)
                  .addItem("Ilda format 0: 3D, palette", 0)
                    .addItem("Ilda format 1: 2D, palette", 1)
                      .addItem("Ilda format 4: 3D, BGR (true colour)", 4)
                        .addItem("Ilda format 5: 2D, BGR (true colour)", 5)
                          .addItem("PIC: single frame, RGB/palette combination", 6)
                            .addItem("CAT: old format (palette only)", 7)
                              .addItem("CAT: new format (palette/RGB combination)", 8)
                                .activate(exportVersion > 3 ? exportVersion - 2 : exportVersion);

  cp5.addToggle("exportWholeFile")
    .setPosition(10, 135)
      .setSize(100, 25)
        .moveTo("export")
          .setValue(exportWholeFile)
            .setMoveable(true)
              .setValueLabel("Show blanking lines")
                .setCaptionLabel("All frames")
                  .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  exportingFrames = cp5.addTextlabel("labelExportingFrames")
    .setText("Exporting from to")
      .setPosition(7, 165)
        .moveTo("export")
          .setVisible(false)
            .setFont(font)
              ;

  cp5.addButton("optSettings")
    .setPosition(width-185, 55)
      .setSize(160, 25)
        .moveTo("export")
          .setLabel("Optimisation settings")
            .setVisible(optimise)
              .getCaptionLabel().alignY(CENTER).alignX(CENTER);
/*
  cp5.addToggle("optimise")
    .setPosition(width-185, 25)
      .setSize(160, 25)
        .setVisible(true)
          .moveTo("export")
            .setValue(optimise)
              .setMoveable(true)
                .setCaptionLabel("Optimise")
                  .getCaptionLabel().alignY(CENTER).alignX(CENTER);
*/


  cp5.addButton("previousPaletteExp")
    .setPosition(width-185, 185)
      .setSize(75, 25)
        .moveTo("export")
          .setLabel("< Previous")
            .setVisible(false)
              ;

  cp5.addButton("nextPaletteExp")
    .setPosition(width-100, 185)
      .setSize(75, 25)
        .moveTo("export")
          .setLabel("Next >")
            .setVisible(false)
              .getCaptionLabel().alignX(RIGHT)
                ;

  cp5.addToggle("findBestColour")
    .setPosition(width-185, 155)
      .setSize(160, 25)
        .setVisible(false)
          .moveTo("export")
            .setValue(includePalette)
              .setMoveable(true)
                .setCaptionLabel("Fit colours")
                  .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addToggle("includePalette")
    .setPosition(width-185, 125)
      .setSize(160, 25)
        .setVisible(false)
          .moveTo("export")
            .setValue(includePalette)
              .setMoveable(true)
                .setCaptionLabel("Include palette")
                  .getCaptionLabel().alignY(CENTER).alignX(CENTER);




  cp5.addTab("about")
    .activateEvent(true)
      .setLabel("About")
        .setId(5);

  /*
    
   //Pro laserist mode no longer supported. Complaints can be sent to the nearest mirror.
   
   //Placed after all Controllers, otherwise nullpointererrors galore
   cp5.addToggle("proLaseristMode")
   .setPosition(width-90, 90)
   .setSize(85, 10)
   .moveTo("default")
   .setValue(proLaseristMode)
   .setVisible(true)
   .setMoveable(true)
   .setValueLabel("Show information")
   .setCaptionLabel("Pro Laserist Mode")
   .getCaptionLabel().alignY(CENTER).alignX(CENTER);
   */
}

