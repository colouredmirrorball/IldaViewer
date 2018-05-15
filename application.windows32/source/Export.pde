
// Fields for Export tab:

RadioButton r;                //ControlP5 radio button (export file format selection)
int exportVersion = 4;        //Create Ilda V4 file by default
FileExport exporter;
boolean exporting = false;    //Exporting mode
Textlabel exportingFrames;    //Some ControlP5 text
boolean includePalette = true;//Should the palette be included in the file?
boolean exportWholeFile = true;//Export all frames by default
boolean findBestColour = true; //RGB points get automatically a fitting palette index
boolean optimise = false;

//      === EXPORT TAB METHODS AND CALLBACKS ===

void beginExporter()
{
  exporting = true;
  if (exporter == null) exporter = new FileExport();

  if (includePalette && isPaletteFile())
  {
    cp5.getController("nextPaletteExp").setVisible(true);
    cp5.getController("previousPaletteExp").setVisible(true);
    if (exportVersion == 0 || exportVersion == 1) cp5.getController("findBestColour").setVisible(true);
  } else
  {
    cp5.getController("nextPaletteExp").setVisible(false);
    cp5.getController("previousPaletteExp").setVisible(false);
    cp5.getController("findBestColour").setVisible(false);
  }

  status.clear();
  status.add("Choose your options then click the big button.");
}

void endExporter()
{
  exporting = false;
  status.clear();
  status.add("Exporter exited");
}

void exportFile()
{
  String pathname;
  String extension = getExtension();
  if (frames.isEmpty()) pathname = extension;
  else pathname = frames.get(frames.size()-1).frameName + extension;
  File theFile = new File(pathname);
  status.clear();
  status.add("Select where to write a file");
  selectOutput("Choose an output location", "ildaOutput", theFile);
}

String getExtension()
{
  String extension = "";
  if (exportVersion == 0 || exportVersion == 1 || exportVersion == 4 || exportVersion == 5) extension = ".ild";
  if (exportVersion == 6) extension = ".pic";
  if (exportVersion == 7 || exportVersion == 8) extension = ".cat";
  return extension;
}

boolean isPaletteFile()
{
  return exportVersion == 0 || exportVersion == 1 || exportVersion == 6 || exportVersion == 7 || exportVersion == 8;
}

void ildaOutput(File selection)
{
  if (selection == null)
  {
    status.clear();
    status.add("File exporting aborted.");
    return;
  }
  status.clear();

  String location = selection.getAbsolutePath();
  char[] test = new char[4];  //Test if it already has the extension .ild:
  for (int i = 0; i < 4; i++)
  {
    test[i] = location.charAt(i+location.length()-4);
  }
  String testing = new String(test);
  String extension = getExtension();
  if ( !testing.equals(extension) )
  {
    location += extension;
  }

  if (exportVersion == 6) exporter.lastFrame = exporter.firstFrame;

  ArrayList<Frame> expframes = new ArrayList<Frame>();

  // You found me! Cheater!
  if (frames.isEmpty() )
  {
    Frame easterEggFrame = new Frame();
    float redPhase = random(0, TWO_PI);      //Colour modulation parameters
    float greenPhase = random(0, TWO_PI);
    float bluePhase = random(0, TWO_PI);
    int xFreq = int(random(1, 15));
    int yFreq = int(random(1, 15));
    int zFreq = int(random(1, 3));
    int redFreq = int(random(1, xFreq*zFreq));
    int greenFreq = int(random(1, yFreq*zFreq));
    int blueFreq = int(random(1, xFreq*yFreq/(5*zFreq)));
    int maxPoints = 499;
    for (int i = 0; i <= maxPoints; i++)
    {
      PVector position = new PVector(
      width*(sin(float(i)*TWO_PI*float(xFreq)/maxPoints)*cos(float(i)*TWO_PI*float(yFreq)/maxPoints)*0.35+0.5), 
      height*(sin(float(i)*TWO_PI*float(xFreq)/maxPoints)*sin(float(i)*TWO_PI*float(yFreq)/maxPoints)*0.35+0.5), 
      depth*(sin(float(i)*TWO_PI*float(xFreq*zFreq)/maxPoints)*cos(float(i)*TWO_PI*2*float(yFreq*zFreq)/maxPoints)*0.35+0.5)
        );

      Point thepoint = new Point(position, int((sin(float(i)*TWO_PI*float(redFreq)/maxPoints+redPhase)*0.5+0.5)*255), int((sin(float(i)*TWO_PI*float(greenFreq)/maxPoints+greenPhase)*0.5+0.5)*255), int((sin(float(i)*TWO_PI*float(blueFreq)/maxPoints+bluePhase)*0.5+0.5)*255), false);
      easterEggFrame.points.add(thepoint);
    }
    easterEggFrame.ildaVersion = 4;
    easterEggFrame.frameName = "EastrEgg";
    easterEggFrame.companyName = "IldaView";
    easterEggFrame.pointCount = 500;
    easterEggFrame.frameNumber = 1;
    easterEggFrame.totalFrames = 1;
    easterEggFrame.scannerHead = 9001;
    easterEggFrame.fitColourIndexWithPalette(getActivePalette());
    expframes.add(easterEggFrame);
    status.add("Laid an egg.");
  } else
  {
    //Only include selected frames:
    if (exporter.firstFrame >= 0 && exporter.lastFrame < frames.size())
    {
      if (exportWholeFile) expframes = frames;
      else
      {
        for (int i = exporter.firstFrame; i <= exporter.lastFrame; i++)
        {
          expframes.add(frames.get(i));
        }
      }
    }
  }

  if (findBestColour)
  {
    for (Frame frame : expframes)
    {
      frame.fitColourIndexWithPalette(getActivePalette());
    }
  }

  ArrayList<Frame> optFrames = new ArrayList<Frame>(expframes.size());
  if (optimise)
  {
    for (int i = 0; i < expframes.size (); i++)
    {
      Frame frame = new Frame(expframes.get(i).clone());
      Frame frame2 = new Frame();

      for (Point p : frame.points)
      {
        frame2.points.add(new Point(p.clone(p)));
      }

      frame2 = frame2.writeProperties(frame, frame2);

      exporter.optimizer.setFrame(frame2);
      exporter.optimizer.optimise();
      frame2 = exporter.optimizer.getFrame();

      optFrames.add(frame2);
    }
  } else optFrames = expframes;

  if (includePalette && (exportVersion == 0 || exportVersion == 1)) 
  {
    FileWrapper theFile = new FileWrapper(location, optFrames, getActivePalette(), exportVersion);
  } else 
  {
    FileWrapper theFile = new FileWrapper(location, optFrames, exportVersion);
  }

  status.add("File exported without errors to location:");
  status.add(location);
}


void exportWholeFile(boolean shouldIt)
{
  if (cp5.getWindow().getCurrentTab().getName().equals("export"))
  {
    if (shouldIt  )
    {
      exporter.firstFrame = 0;
      exporter.lastFrame = frames.size()-1;
      if (frames.isEmpty() ) exporter.lastFrame = 0;

      cp5.getController("exportWholeFile").setCaptionLabel("All frames");
      exportingFrames.setVisible(false);
    } else
    {
      exporter.displayFrames();
      exportingFrames.setVisible(true);
      exportingFrames.setText("Exporting from " + (exporter.firstFrame+1) + " to " + (exporter.lastFrame+1));
      cp5.getController("exportWholeFile").setCaptionLabel("Selected frames");
      status.clear();
      status.add("Left click on the first frame, right click on the last frame. Drag with middle mouse button to scroll.");
    }
    exporter.exportAllFrames = shouldIt;
  }
  exportWholeFile = shouldIt;
}



void includePalette(boolean shouldWe)
{
  includePalette = shouldWe;
  if (shouldWe)
  {
    cp5.getController("nextPaletteExp").setVisible(true);
    cp5.getController("previousPaletteExp").setVisible(true);
    cp5.getController("findBestColour").setVisible(true);
    cp5.getController("includePalette").getCaptionLabel().setText("Include palette");
  } else
  {
    cp5.getController("nextPaletteExp").setVisible(false);
    cp5.getController("previousPaletteExp").setVisible(false);
    cp5.getController("findBestColour").setVisible(false);
    cp5.getController("includePalette").getCaptionLabel().setText("Don't include palette");
  }
}

void previousPaletteExp() //manually skip to previous active palette
{
  if (activePalette <= 0)
  {
    activePalette = palettes.size()-1;
  } else 
  {
    activePalette--;
  }
}

void nextPaletteExp() //manually skip to next active palette
{
  if (activePalette >= palettes.size()-1)
  {
    activePalette = 0;
  } else
  {
    activePalette++;
  }
}

void findBestColour(boolean doIttt)
{
  if (doIttt) cp5.getController("findBestColour").getCaptionLabel().setText("Fit colours");
  else cp5.getController("findBestColour").getCaptionLabel().setText("Don't fit colours");
}

void optimise(boolean o)
{
  cp5.getController("optSettings").setVisible(o);
}

void optSettings()
{
  if (!exporter.optimizer.guiVisible)
  {
    OptimizerOverlay oo = new OptimizerOverlay(exporter.optimizer, 50, 50, 480, 320);
    PFrame f = new PFrame(oo, 480, 320);
  }
}



class FileExport
{
  int firstFrame = 0;
  int lastFrame = frames.size()-1;
  boolean exportAllFrames = true;
  PGraphics framelist;
  PGraphics overlay;
  int framesSizeX = 100;
  int frameSize = framesSizeX-10;
  int frameListSizeY = height - 190;
  int yoffset = 0;
  int framesYPos = 180;
  int prevYOffset = -1;
  int visibleFrames = frameListSizeY/(framesSizeX-10);
  boolean scrolling = false;       //scrolling with middle mouse button
  boolean startScrolling = false;  //scrolling with scrollbar
  Optimizer optimizer;
  boolean showOptSettings = false;

  FileExport()
  {


    optimizer = new Optimizer();
  }

  void update()
  {
    if (showOptSettings)
    {
      //optimizer.update();
      //optimizer.display();
    }



    if (includePalette && (exportVersion == 0 || exportVersion == 1) || exportVersion == 6 || exportVersion == 7 || exportVersion == 8)
    {
      if (activePalette >= 0  || activePalette < palettes.size())
      {
        displayPalette(palettes.get(activePalette));
      }
      fill(255);
      text(palettes.get(activePalette).name, width-185, 395);
    }

    if (!exportAllFrames)
    {
      if (yoffset != prevYOffset)
      {

        framelist.beginDraw();
        framelist.fill(50);
        framelist.noStroke();
        framelist.rect(0, 0, framesSizeX-1, framelist.height-1);

        if (!frames.isEmpty())
        {
          //println(max ( (int) -yoffset/frameSize-2, 0), min(frames.size(), (int) -yoffset/frameSize+visibleFrames+1));
          for (int i = max ( (int) -yoffset/(frameSize+5)-1, 0); i < min(frames.size(), (int) -yoffset/(frameSize+5)+visibleFrames+2); i++)
          {
            framelist.noStroke();

            if (i == firstFrame)
            {
              framelist.strokeWeight(3);
              framelist.stroke(255, 255, 0);
            }

            if (i == lastFrame && exportVersion != 6)
            {
              framelist.strokeWeight(3);
              framelist.stroke(0, 255, 255);
            }

            if ( i< lastFrame && i > firstFrame && exportVersion != 6)
            {
              framelist.strokeWeight(2); 
              framelist.stroke(lerp(0, 255, 1-((float)i-(float)firstFrame)/(lastFrame)), 255, lerp(0, 255, ((float)i-(float)firstFrame)/(lastFrame)));
            }

            framelist.fill(0);
            framelist.rect(5, yoffset+(frameSize+5)*i, 90, 90);
            frames.get(i).drawFrame(framelist, 5, (float)yoffset+(frameSize+5)*i, 0.0, 90.0, 90.0, 0.0, false, true);
            if (i == firstFrame)
            {
              framelist.fill(255, 50);
              framelist.textFont(f16);
              framelist.text("First", 7, yoffset+(frameSize+5)*i+15);
            }
            if (i == lastFrame)
            {
              framelist.fill(255, 50);
              framelist.textFont(f16);
              framelist.text("Last", 7, yoffset+(frameSize+5)*i+75);
            }
          }
        } else
        {
          framelist.textFont(f16);
          framelist.fill(255, 50);
          framelist.text("No frames", 5, 55);
        }
        framelist.endDraw();
      }

      firstFrame = constrain(firstFrame, 0, frames.size()-1);
      lastFrame = constrain(lastFrame, 0, frames.size()-1);

      imageMode(CORNER);

      image(framelist, 10, framesYPos);
      image(overlay, 10, framesYPos);


      if (mousePressed && mouseX > 10 && mouseX < 10+framesSizeX && mouseY > framesYPos)
      {
        int chosenFrame = (int) (mouseY-yoffset-framesYPos)/(frameSize+5);
        if (mouseButton == LEFT) 
        {
          firstFrame = min(max(0, chosenFrame), frames.size()-1);
          exportingFrames.setText("From " + (exporter.firstFrame+1) + " to " + (exporter.lastFrame+1));
        }
        if (mouseButton == CENTER && !scrolling) 
        {
          scrolling = true;
          prevYOffset = -yoffset + mouseY;
          cursor(MOVE);
        }
        if (mouseButton == RIGHT) 
        {
          lastFrame = min(max(0, chosenFrame), frames.size()-1);
          exportingFrames.setText("From " + (exporter.firstFrame+1) + " to " + (exporter.lastFrame+1));
        }
      }

      if (!mousePressed && scrolling)
      {
        scrolling = false;
        cursor(ARROW);
      }

      if (scrolling)
      {
        yoffset = mouseY - prevYOffset;
      }

      float scrollY = map(yoffset, 0, -frames.size() * (frameSize+5) + (frameSize+5) * visibleFrames, framesYPos+45, height-70);

      fill(127);
      noStroke();
      rect(113, framesYPos, 20, 20);
      rect(113, framesYPos+22, 20, 20);
      rect(113, height-60, 20, 20);
      rect(113, height-38, 20, 20);
      rect(113, scrollY, 20, 7);
      fill(0);
      text("^", 119, framesYPos+15);
      text("^", 119, framesYPos+21);
      text("^", 119, framesYPos+40);
      text("v", 119, height-45);
      text("v", 119, height-26);
      text("v", 119, height-20);

      if (mousePressed && mouseX > 115 && mouseX < 135)
      {
        if (inBetween(mouseY, framesYPos, framesYPos+20)) yoffset = 0;
        if (inBetween(mouseY, framesYPos+22, framesYPos+42)) yoffset+=5;
        if (inBetween(mouseY, height-60, height-40)) yoffset-=5;
        if (inBetween(mouseY, height-38, height-18)) yoffset = -frames.size() * (frameSize+5) + (frameSize+5) * visibleFrames;
        if (inBetween(mouseY, scrollY, scrollY+7)) startScrolling = true;
      }

      if (startScrolling)
      {
        yoffset = (int) map(mouseY, framesYPos+45, height-70, 0, -frames.size() * (frameSize+5) + (frameSize+5) * visibleFrames);
      }

      if (frames.size() > visibleFrames) yoffset = constrain(yoffset, -frames.size() * (frameSize+5) + (frameSize+5) * visibleFrames, 0);
      else yoffset = 0;

      if (startScrolling && !mousePressed) startScrolling = false;
    }
  }

  void displayFrames()
  {
    framelist = createGraphics(100, frameListSizeY, P3D);
    overlay = createGraphics(100, frameListSizeY);
    overlay.beginDraw();
    for (int i = 0; i < 30; i++)
    {
      overlay.stroke(red(backgroundcolour), green(backgroundcolour), blue(backgroundcolour), 255-i*8.5);
      overlay.line(0, i, framesSizeX, i);
      overlay.line(0, frameListSizeY-i, framesSizeX, frameListSizeY-i);
    }
    overlay.endDraw();
  }

  void displayPalette(Palette palette)
  {
    int i = 0;
    for (PaletteColour colour : palette.colours)
    {
      stroke( color(50, 50, 50));
      strokeWeight(1);

      colour.displayColour(width-185+10*(i%16), 215+10*int(i/16), 10);
      i++;
    }
  }
}

