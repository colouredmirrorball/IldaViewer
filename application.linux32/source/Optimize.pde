


class OptimizerOverlay extends OverlayImage
{
  Optimizer optimiser;
  int w, h;
  OptimizerOverlay(Optimizer opt, int x, int y, int w, int h)
  {
    super("Optimisation settings", x, y);
    optimiser = opt;
    optimiser.guiVisible = true;
  }

  public void setup()
  {
    size(480, 320, P2D);
    smooth();
    cp5 = new ControlP5(this);

    frame.setSize(width+1, height+1);  //What the actual fuck?! 

    cp5.setFont(f10);
    cp5.enableShortcuts(); //Enables keyboard shortcuts of ControlP5 controls
    cp5.setColorLabel(textcolour);
    cp5.setColorBackground(buttoncolour);
    cp5.setColorActive(activecolour);
    cp5.setColorForeground(mouseovercolour);

    cp5.setBroadcast(false);

    cp5.addToggle("removeCollinear")
      .setPosition(5, 10)
        .setSize(125, 20)
          .setCaptionLabel("Remove collinear points")
            .getCaptionLabel().alignY(CENTER).alignX(CENTER).toUpperCase(false);

    cp5.addToggle("removeSame")
      .setPosition(150, 10)
        .setSize(125, 20)
          .setCaptionLabel("Remove identical points")
            .getCaptionLabel().alignY(CENTER).alignX(CENTER).toUpperCase(false);

    cp5.addToggle("removeBlanked")
      .setPosition(300, 10)
        .setSize(150, 20)
          .setCaptionLabel("Remove stray blanked points")
            .getCaptionLabel().alignY(CENTER).alignX(CENTER).toUpperCase(false);

    cp5.addToggle("interpolateLit")
      .setPosition(5, 50)
        .setSize(125, 20)
          .setCaptionLabel("Interpolate lit points")
            .getCaptionLabel().alignY(CENTER).alignX(CENTER).toUpperCase(false);


    cp5.addSlider("interpolationDistance")
      .setPosition(150, 50)
        .setSize(300, 20)
          .setRange(0.0001, 1.5)
            .setCaptionLabel("Interpolation distance")
              .getCaptionLabel().alignY(CENTER).alignX(CENTER).toUpperCase(false);

    cp5.addToggle("interpolateBlanked")
      .setPosition(5, 80)
        .setSize(125, 20)
          .setCaptionLabel("Interpolate blanked points")
            .getCaptionLabel().alignY(CENTER).alignX(CENTER).toUpperCase(false);


    cp5.addSlider("interpolationBlDistance")
      .setPosition(150, 80)

        .setSize(300, 20)
          .setRange(0.0001, 1.5)
            .setCaptionLabel("Interpolation distance")
              .getCaptionLabel().alignY(CENTER).alignX(CENTER).toUpperCase(false);


    cp5.addButton("load")
      .setPosition(5, height-75)
        .setSize(125, 20)
          .setCaptionLabel("Load")
            .getCaptionLabel().alignY(CENTER).alignX(CENTER).toUpperCase(false);

    cp5.addButton("save")
      .setPosition(150, height-75)
        .setSize(125, 20)
          .setCaptionLabel("Save")
            .getCaptionLabel().alignY(CENTER).alignX(CENTER).toUpperCase(false);

    cp5.addButton("def")
      .setPosition(295, height-75)
        .setSize(125, 20)
          .setCaptionLabel("Default")
            .getCaptionLabel().alignY(CENTER).alignX(CENTER).toUpperCase(false);

    updateGuiValues();

    cp5.setBroadcast(true);
  }


  public void updateGuiValues()
  {
    cp5.getController("removeCollinear").setValue(optimiser.removeCollinear ? 1f : 0f);
    cp5.getController("removeSame").setValue(optimiser.removeIdentical ? 1f : 0f);
    cp5.getController("removeBlanked").setValue(optimiser.removeBlanked ? 1f : 0f);
    cp5.getController("interpolateLit").setValue(optimiser.interpolateLit ? 1f : 0f);
    cp5.getController("interpolationDistance").setValue(optimiser.interpolationDistance);
    cp5.getController("interpolateBlanked").setValue(optimiser.interpolateBlanked ? 1f : 0f);
    cp5.getController("interpolationBlDistance").setValue(optimiser.interpolateBlDistance);
  }

  public void load()
  {
    optimiser.load();
    updateGuiValues();
  }

  public void save()
  {
    optimiser.save();
  }

  public void def()
  {
    optimiser.def();
    updateGuiValues();
  }

  public void removeCollinear(boolean value)
  {
    optimiser.removeCollinear = value;
  }

  public void removeSame(boolean value)
  {
    optimiser.removeIdentical = value;
  }

  public void removeBlanked(boolean value)
  {
    optimiser.removeBlanked = value;
  }


  public void interpolateLit(boolean value)
  {
    optimiser.interpolateLit = value;
  }

  public void interpolationDistance(float value)
  {
    optimiser.interpolationDistance = value;
  }

  public void interpolateBlanked(boolean value)
  {
    optimiser.interpolateBlanked = value;
  }

  public void interpolationBlDistance(float value)
  {
    optimiser.interpolateBlDistance = value;
  }

  public void draw()
  {

    background(backgroundcolour);
  }

  public void windowClosed()
  {
    optimiser.guiVisible = false;
  }
}


class Optimizer
{
  Frame frame;
  String[] settings = new String[0];
  boolean guiVisible = false;

  boolean removeCollinear = true;
  boolean removeIdentical = true;
  boolean removeBlanked = true;

  boolean interpolateLit = true;
  float interpolationDistance = 1f;

  boolean interpolateBlanked = true;
  float interpolateBlDistance = 1f;

  void optimise()
  {
    try
    {
      if (removeCollinear || removeIdentical || removeBlanked)
      {
        for (int i = frame.points.size ()-2; i >= 0; i--)
        {
          Point prevp = frame.points.get(i+1);
          Point p = frame.points.get(i);
          Point nextp = null;
          if (i != 0) nextp = frame.points.get(i-1);
          if (prevp == null || p == null || nextp == null) break;

          if (nextp.equals(p)) frame.points.remove(i);
          else
          {

            float xverh = (prevp.position.x-p.position.x)/(nextp.position.x-p.position.x);
            float yverh = (prevp.position.y-p.position.y)/(nextp.position.y-p.position.y);
            float zverh = (prevp.position.z-p.position.z)/(nextp.position.z-p.position.z);
            //println(xverh, yverh, zverh);
            if (xverh == yverh && yverh == zverh) frame.points.remove(i);
          }
        }
      }



      if (interpolateLit || interpolateBlanked)
      {
        float maxdistsqb = sq(interpolateBlDistance*width);
        float maxdistsql = sq(interpolationDistance*width);
        for (int i = frame.points.size ()-2; i >= 0; i--)
        {
          Point prevp = frame.points.get(i+1);
          Point p = frame.points.get(i);
          Point nextp = null;
          if (i != 0) nextp = frame.points.get(i-1);

          float dpsq = (prevp.position.x - p.position.x) * (prevp.position.x - p.position.x) + (prevp.position.y - p.position.y) * (prevp.position.y - p.position.y) + (prevp.position.z - p.position.z) * (prevp.position.z - p.position.z);
          float dnsq = 0;
          if (nextp != null) dnsq = (nextp.position.x - p.position.x) * (nextp.position.x - p.position.x) + (nextp.position.y - p.position.y) * (nextp.position.y - p.position.y) + (nextp.position.z - p.position.z) * (nextp.position.z - p.position.z);


          if ((prevp.blanked && dpsq > maxdistsqb && interpolateBlanked) || (!prevp.blanked && dpsq > maxdistsql && interpolateLit)) {
            float dist = sqrt(dpsq);
            float maxDist = prevp.blanked ? interpolateBlDistance*width : interpolationDistance*width;
            int addedPoints = (int) (dist/maxDist);
            for (int j = 1; j <= addedPoints; j++)
            {
              //println(i, frame.points.size(), j, addedPoints);
              Point newp = new Point(prevp);
              float factor = (1 - (dist - j * maxDist) / dist);
              newp.position.x = prevp.position.x + (p.position.x - prevp.position.x) * factor;
              newp.position.y = prevp.position.y + (p.position.y - prevp.position.y) * factor;
              newp.position.z = prevp.position.z + (p.position.z - prevp.position.z) * factor;
              frame.points.add(i + 1, newp);
            }
          }
        }
      }
    }
    catch(Exception e)
    {
      println(e);
    }
  }

  void setFrame(Frame frame)
  {
    this.frame = new Frame(frame);
  }

  Frame getFrame()
  {
    return frame;
  }

  void setSettings(String[] settings)
  {
    this.settings = settings;
    for (String s : settings)
    {
      String[] sub = splitTokens(s);
      try
      {
        if (sub.length > 1)
        {
          if (sub[0].equals("removeCollinear")) removeCollinear = boolean(sub[1]);
          if (sub[0].equals("removeIdentical")) removeIdentical = boolean(sub[1]);
          if (sub[0].equals("removeBlanked")) removeBlanked = boolean(sub[1]);
          if (sub[0].equals("interpolateLit")) interpolateLit = boolean(sub[1]);
          if (sub[0].equals("interpolationDistance")) interpolationDistance = float(sub[1]);
          if (sub[0].equals("interpolateBlanked")) interpolateBlanked = boolean(sub[1]);
          if (sub[0].equals("interpolateBlDistance")) interpolateBlDistance = float(sub[1]);
        }
      }
      catch(Exception e)
      {
        status.clear();
        status.add("Error when trying to set optimisation settings.");
      }
    }
    println(settings);
  }

  String[] getSettingsFile()
  {
    String[] settings = new String[7];
    settings[0] = "removeCollinear " + removeCollinear;
    settings[1] = "removeIdentical " + removeIdentical;
    settings[2] = "interpolateLit " + interpolateLit;
    settings[3] = "interpolationDistance " + interpolationDistance;
    settings[4] = "interpolateBlanked " + interpolateBlanked;
    settings[5] = "interpolateBlDistance " + interpolateBlDistance;
    settings[6] = "removeBlanked " + removeBlanked;



    return settings;
  }

  public void load()
  {
    selectInput("Select an optimisation settings file (.opt)", "optFileSelected");
  }

  public void save()
  {
    String pathname;
    pathname = ".opt";
    File theFile = new File(pathname);
    status.clear();
    status.add("Select where to save the optimisation settings file");
    selectOutput("Select where to save an optimisation settings file", "optFileOutput", theFile);
  }

  public void def()
  {
    boolean removeCollinear = true;
    boolean removeIdentical = true;
    boolean removeBlanked = true;

    boolean interpolateLit = true;
    float interpolationDistance = 1;

    boolean interpolateBlanked = true;
    float interpolateBlDistance = 1;
  }

  public void optFileSelected(File selection)
  {
    if (selection == null)
    {
      status.clear();
      status.add("Optimisation file loading aborted or no valid file selected.");
      return;
    }

    try
    {
      setSettings(loadStrings(selection));
    }
    catch(Exception e)
    {
      status.clear();
      status.add("Error when trying to parse optimisation settings file.");
    }
  }

  public void optFileOutput(File selection)
  {
    if (selection == null)
    {
      status.clear();
      status.add("Optimisation settings not saved.");
      return;
    }
    String location = selection.getAbsolutePath();
    char[] test = new char[4];  //Test if it already has the extension .osc:
    for (int i = 0; i < 4; i++)
    {
      test[i] = location.charAt(i+location.length()-4);
    }
    String testing = new String(test);
    if ( !testing.equals(".opt") )
    {
      location += ".opt";
    }

    saveStrings(location, getSettingsFile());

    status.add("Optimisation settings file was saved to:");
    status.add(location);
  }
}

