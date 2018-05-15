import javax.swing.*;
import java.awt.event.*;

ArrayList<OverlayImage> overlayImages = new ArrayList<OverlayImage>();


class PFrame extends JFrame
{
  OverlayImage p;
  PFrame(OverlayImage p, int width, int height)
  {
    this.p = p;
    show();
    p.init();
    removeNotify();
    setUndecorated(false);
    addNotify();
    setBounds(100, 100, width, height);
    
    add(p);
    p.frame = this;
    
    
    setTitle(p.title);
    //setResizable(false);
    try
    {
      if (icon == null) 
      {
        icon = createGraphics(256, 256);
        icon.beginDraw();
        icon.image(loadImage("Images/Icon2.png"), 0, 0);
        icon.endDraw();
      }

      setIconImage(icon.image);
    }
    catch(Exception e)
    {
    }
  }

  public void processWindowEvent(WindowEvent object)
  {
    if (object.getID() == WindowEvent.WINDOW_CLOSING)
    {
      p.windowClosed();
      dispose();
    }
  }
}


class OverlayImage extends PApplet
{

  ControlP5 cp5;
  String title;
  float x, y;
  OverlayImage(String name, float x, float y)
  {
    title = name;
    this.x = x;
    this.y = y;
  }


  public void setup()
  {
    cp5 = new ControlP5(this);
  }

  public void draw()
  {
    background(backgroundcolour);
  }

  public void windowClosed()
  {
  }
}

