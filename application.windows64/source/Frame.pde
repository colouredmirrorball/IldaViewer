
class Frame
{

  /*
   * A Frame stores all its Points and has some methods to display them
   * and to save header data
   */

  ArrayList<Point> points = new ArrayList<Point>();
  //The Points in the Frame


  int ildaVersion;    //Data retrieved from header
  String frameName;
  String companyName;
  int pointCount;
  int frameNumber;
  int totalFrames;
  int scannerHead;
  StringList hoofding = new StringList(); //Data now in a nice StringList to display 
  boolean palette = false;

  Frame()
  {
  }

  Frame(Frame frame)
  {
    this.points = new ArrayList<Point>(frame.points.size());
    for (Point point : frame.points)
    {
      this.points.add(point.clone(point));
    }
    this.ildaVersion = frame.ildaVersion;
    if (frame.frameName != null) this.frameName = new String(frame.frameName);
    else frameName = "";
    if (frame.companyName != null) this.companyName = new String(frame.companyName);
    else companyName = "IldaViewer";
    this.pointCount = frame.pointCount;
    this.frameNumber = frame.frameNumber;
    this.totalFrames = frame.totalFrames;
    this.scannerHead = frame.scannerHead;
    this.palette = frame.palette;
  }
  
  Frame writeProperties(Frame frame1, Frame frame2)
  {
    frame2.ildaVersion = frame1.ildaVersion;
    frame2.frameName = frame1.frameName;
    frame2.companyName = frame1.companyName;
    frame2.pointCount = frame1.pointCount;
    frame2.frameNumber = frame1.frameNumber;
    frame2.totalFrames = frame1.totalFrames;
    frame2.scannerHead = frame1.scannerHead;
    frame2.palette = frame1.palette;
    
    return frame2;
  }

  Frame clone()
  {
    Frame frame = new Frame(this);
    return frame;
  }


  //This method changes the colour of each point according to the active palette
  void palettePaint(Palette palette)
  {
    for ( Point point : points)
    {
      point.setColourFromPalette(palette);
    }
  }

  //This puts all the header information inside the hoofding StringList

  void formHeader()
  {
    hoofding.clear();
    hoofding.append("Ilda version " + ildaVersion);
    hoofding.append("Frame: " + frameName);
    hoofding.append("Company: " + companyName);
    hoofding.append("Point count: " + pointCount);
    hoofding.append("Frame number: " + frameNumber);
    hoofding.append("Total frames: " + totalFrames);
    hoofding.append("Scanner head: " + scannerHead);
  }

  //Returns the real point count

  int getPointCount()
  {
    return points.size();
  }

  //By default, draw the frame with visible blanking points
  void drawFrame()
  {
    drawFrame(true);
  }

  //Displays all the points inside this frame with a line in between them:



  void drawFrame(boolean showBlankedPoints)
  {

    if (!loading)
    {
      boolean firstPoint = true;
      float oldpositionx = 0; 
      float oldpositiony = 0;
      float oldpositionz = 0;
      for (Point point : points)
      {
        if (showBlankedPoints || !point.blanked) point.displayPoint();


        PVector position = point.getPosition();

        if (!firstPoint)
        {
          strokeWeight(1);  
          if (!showBlankedPoints && point.blanked) stroke(0);   
          else
          {   
            line(position.x, position.y, position.z, oldpositionx, oldpositiony, oldpositionz);
          }
          oldpositionx = position.x;
          oldpositiony = position.y;
          oldpositionz = position.z;
        } else
        {
          firstPoint = false;
          oldpositionx = position.x;
          oldpositiony = position.y;
          oldpositionz = position.z;
        }
      }
    } else println("Warning: tried to display while loading file");
  }

  void drawFrame(float offX, float offY, float offZ, float sizeX, float sizeY, float sizeZ)
  {
    drawFrame(g, offX, offY, offZ, sizeX, sizeY, sizeZ);
  }

  void drawFrame(PGraphics pg, float offX, float offY, float offZ, float sizeX, float sizeY, float sizeZ)
  {
    drawFrame(pg, offX, offY, offZ, sizeX, sizeY, sizeZ, true, true);
  }


  void drawFrame(float offX, float offY, float offZ, float sizeX, float sizeY, float sizeZ, boolean showBlankedPoints, boolean clipping)
  {
    drawFrame(g, offX, offY, offZ, sizeX, sizeY, sizeZ, showBlankedPoints, clipping);
  }

  void drawFrame(PGraphics pg, float offX, float offY, float offZ, float sizeX, float sizeY, float sizeZ, boolean showBlankedPoints, boolean clipping)
  {
    //pg.beginDraw();
    boolean firstPoint = true;
    float oldpositionx = 0; 
    float oldpositiony = 0;
    float oldpositionz = 0;
    for (Point point : points)
    {
      PVector position = point.getPosition();

      float x = map(position.x, 0, width, 0, sizeX) + offX;
      float y = map(position.y, 0, height, 0, sizeY) + offY;
      float z = map(position.z, 0, depth, 0, sizeZ) + offZ;

      if (clipping)
      {
        if (x < offX) x = offX;
        if (y < offY) y = offY;
        if (z < offZ) z = offZ;
        if (x > offX + sizeX) x = offX + sizeX;
        if (y > offY + sizeY) y = offY + sizeY;
        if (z > offZ + sizeZ) z = offZ + sizeZ;
      }

      if (showBlankedPoints || !point.blanked)
      {
        pg.strokeWeight(3);
        pg.stroke(point.colour);
        if (point.blanked) pg.stroke(75, 75, 75);
        pg.point(x, y, z);
      }



      if (!firstPoint)
      {
        pg.strokeWeight(1);  
        if (!showBlankedPoints && point.blanked) pg.stroke(0);   
        else
        {   
          pg.line(x, y, z, oldpositionx, oldpositiony, oldpositionz);
        }
        oldpositionx = x;
        oldpositiony = y;
        oldpositionz = z;
      } else
      {
        firstPoint = false;
        oldpositionx = x;
        oldpositiony = y;
        oldpositionz = z;
      }
    }
    //pg.endDraw();
  }

  //Translates all points with a PVector
  void translate(PVector newposition)
  {
    for (Point point : points)
    {
      point.position.add(newposition);
    }
  }

  void fitColourIndexWithPalette(Palette palette)
  {
    for (Point point : points)
    {
      point.paletteIndex = point.getBestFittingPaletteColourIndex(palette);
    }
  }

  void merge(Frame frame)
  {
    if (frame.points != null && frame.points.size() > 0) 
    {
      Point p = frame.points.get(frame.points.size()-1);
      p.blanked = true;
      points.add(p);
    }
    points.addAll(frame.points);
  }

  void merge(Frame frame, float[] mergedInformation)
  {
    if (mergedInformation.length != 10) return;

    // 0: frame header
    if (mergedInformation[0] != 0)
    {
      ildaVersion = frame.ildaVersion;
      frameName = frame.frameName;
      companyName = frame.companyName;
      frameNumber = frame.frameNumber;
      scannerHead = frame.scannerHead;
    }

    // 1: Point number
    if (mergedInformation[1] != 0)
    {
      if (points.size() < frame.points.size())
      {
        for (int i = points.size (); i < frame.points.size(); i++)
        {
          points.add(frame.points.get(i));
        }
      } else
      {
        for (int i = points.size ()-1; i >= frame.points.size(); i--)
        {
          points.remove(i);
        }
      }
    }

    // 2: X
    if (mergedInformation[2] != 0)
    {
      for (int i = 0; i < min (points.size (), frame.points.size()); i++)
      {
        points.get(i).position.x = frame.points.get(i).position.x;
      }
    }

    // 3: R
    if (mergedInformation[3] != 0)
    {
      for (int i = 0; i < min (points.size (), frame.points.size()); i++)
      {
        points.get(i).colour = color((frame.points.get(i).colour >> 16) & 0xFF, (points.get(i).colour >> 8) & 0xFF, points.get(i).colour & 0xFF);
      }
    }

    // 4: Y
    if (mergedInformation[4] != 0)
    {
      for (int i = 0; i < min (points.size (), frame.points.size()); i++)
      {
        points.get(i).position.y = frame.points.get(i).position.y;
      }
    }

    // 5: G
    if (mergedInformation[5] != 0)
    {
      for (int i = 0; i < min (points.size (), frame.points.size()); i++)
      {
        points.get(i).colour = color((points.get(i).colour >> 16) & 0xFF, (frame.points.get(i).colour >> 8) & 0xFF, points.get(i).colour & 0xFF);
      }
    }

    // 6: Z
    if (mergedInformation[6] != 0)
    {
      for (int i = 0; i < min (points.size (), frame.points.size()); i++)
      {
        points.get(i).position.z = frame.points.get(i).position.z;
      }
    }

    // 7: B
    if (mergedInformation[7] != 0)
    {
      for (int i = 0; i < min (points.size (), frame.points.size()); i++)
      {
        points.get(i).colour = color((points.get(i).colour >> 16) & 0xFF, (points.get(i).colour >> 8) & 0xFF, frame.points.get(i).colour & 0xFF);
      }
    }

    // 8: blanking
    if (mergedInformation[8] != 0)
    {
      for (int i = 0; i < min (points.size (), frame.points.size()); i++)
      {
        points.get(i).blanked = frame.points.get(i).blanked;
      }
    }

    // 9: palette index
    if (mergedInformation[9] != 0)
    {
      for (int i = 0; i < min (points.size (), frame.points.size()); i++)
      {
        points.get(i).paletteIndex = frame.points.get(i).paletteIndex;
      }
    }

    pointCount = points.size();
    formHeader();
  }

  String toString()
  {
    return "Name: " + frameName + "  | Company: " + companyName + "  | Points: " + pointCount;
  }
}

float[][] calculateRotationMatrix(float theta, float phi, float psi)
{
  float[][] R = new float[3][3]; //Rotation matrix

  //First row:
  R[0][0] = cos(phi)*cos(psi);
  R[0][1] = cos(phi)*sin(psi);
  R[0][2] = -sin(phi);

  //Second row:
  R[1][0] = sin(theta)*sin(phi)*cos(psi) - cos(theta)*sin(psi);
  R[1][1] = sin(theta)*sin(phi)*sin(psi) + cos(theta)*cos(psi);
  R[1][2] = sin(theta)*cos(phi);

  //Third row:
  R[2][0] = cos(theta)*sin(phi)*cos(psi) + sin(theta)*sin(psi);
  R[2][1] = cos(theta)*sin(phi)*sin(psi) - sin(theta)*cos(psi);
  R[2][2] = cos(theta)*cos(phi);

  return R;
}

