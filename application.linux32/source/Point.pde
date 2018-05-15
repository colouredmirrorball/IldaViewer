class Point
{

  /* 
   * Points are always 3D and with RGB colour variables.
   */

  PVector position;
  color colour;
  boolean blanked;
  int paletteIndex;


  Point( PVector _position, int red, int green, int blue, boolean _blanked)
  {
    position = _position;
    colour = color(red, green, blue);
    blanked = _blanked;
    //palette = false;
  }
  Point(float x, float y, float z, int red, int green, int blue, boolean _blanked)
  {
    position = new PVector(x, y, z);
    colour = color(red, green, blue);
    blanked = _blanked;
    //palette = false;
  }

  Point(float x, float y, float z, int _paletteIndex, boolean _blanked)
  {
    position = new PVector(x, y, z);
    paletteIndex = _paletteIndex;
    blanked = _blanked;
    //palette = true;
  }

  //        !!! Notice how this constructor is different from the previous ones, 
  //            this is because "color" is actually an int and otherwise 
  //            there would be two Point(PVector,int,boolean) constructors...

  Point(PVector _position, boolean _blanked, int _paletteIndex)
  {
    position = _position;
    paletteIndex = _paletteIndex;
    blanked = _blanked;
    //palette = true;
  }

  Point(int x, int y, int z, int _paletteIndex, boolean _blanked)
  {
    position = new PVector(x, y, z);
    paletteIndex = _paletteIndex;
    blanked = _blanked;
  }

  Point(float x, float y, float z, boolean _blanked, color theColour)
  {
    position = new PVector(x, y, z);
    colour = theColour;
    blanked = _blanked;
    //palette = false;
  }

  Point(Point point)
  {
    this.position = new PVector(point.position.x, point.position.y, point.position.z);
    this.colour = point.colour;
    this.blanked = point.blanked;
    this.paletteIndex = point.paletteIndex;
  }

  Point clone()
  {
    Point point = new Point(this);
    return point;
  }

  Point clone(Point point)
  {
    Point newPoint = new Point(point);
    return newPoint;
  }

  void displayPoint()
  {
    strokeWeight(3);
    stroke(colour);
    if (blanked) stroke(75, 75, 75);
    point(position.x, position.y, position.z);
  }

  int getBestFittingPaletteColourIndex(Palette palette)
  {
    int index = 0;
    float distance = 1000;
    PVector colourPos = new PVector((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF);
    int i = 0;
    for (PaletteColour theColour : palette.colours)
    {
      PVector palColourPos = new PVector(theColour.red, theColour.green, theColour.blue);
      float d = colourPos.dist(palColourPos);
      if ( d < distance)
      {
        distance = d;
        index = i;
      }
      i++;
    }
    return index;
  }

  PVector getPosition()
  {
    return position;
  }

  void setColourFromPalette(Palette palette)
  {
    setColourFromPalette(palette, paletteIndex);
  }

  void setColourFromPalette(Palette palette, int index)
  {
    colour = palette.getColour(index);
  }


  boolean equals(Point p)
  {
    return position.x == p.position.x && position.y == p.position.y && position.z == p.position.z && colour == p.colour && paletteIndex == p.paletteIndex && p.blanked == blanked;
  }
}

