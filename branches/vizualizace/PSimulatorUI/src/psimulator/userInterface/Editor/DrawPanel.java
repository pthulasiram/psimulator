package psimulator.userInterface.Editor;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.undo.UndoManager;
import psimulator.dataLayer.ColorMixerSignleton;
import psimulator.userInterface.ActionListeners.DrawPanelListenerStrategy;
import psimulator.userInterface.ActionListeners.DrawPanelListenerStrategyAddCable;
import psimulator.userInterface.ActionListeners.DrawPanelListenerStrategyAddHwComponent;
import psimulator.userInterface.ActionListeners.DrawPanelListenerStrategyHand;
import psimulator.userInterface.Editor.Actions.ActionOnDelete;
import psimulator.userInterface.Editor.Components.AbstractComponent;
import psimulator.userInterface.Editor.Components.AbstractHwComponent;
import psimulator.userInterface.Editor.Enums.Tools;
import psimulator.userInterface.MainWindowInterface;
import psimulator.userInterface.imageFactories.AbstractImageFactory;

/**
 *
 * @author Martin
 */
public class DrawPanel extends JPanel implements Observer {
    // mouse listeners

    private DrawPanelListenerStrategy mouseListenerHand;
    private DrawPanelListenerStrategy mouseListenerAddHwComponent;
    private DrawPanelListenerStrategy mouseListenerCable;
    private DrawPanelListenerStrategy currentMouseListener;
    private Graph graph = new Graph();
    private UndoManager undoManager = new UndoManager();
    private ZoomManager zoomManager = new ZoomManager();
    private AbstractImageFactory imageFactory;
    private MainWindowInterface mainWindow;
    private boolean lineInProgres = false;
    private Point lineStart;
    private Point lineEnd;
    private List<AbstractComponent> markedCables = new ArrayList<AbstractComponent>();
    private List<AbstractComponent> markedComponents = new ArrayList<AbstractComponent>();
    private Dimension defaultZoomAreaMin = new Dimension(800, 600);
    private Dimension defaultZoomArea = new Dimension(defaultZoomAreaMin);
    private Dimension area = new Dimension(defaultZoomArea);

    public DrawPanel(MainWindowInterface mainWindow, AbstractImageFactory imageFactory) {
        super();

        this.mainWindow = mainWindow;
        this.imageFactory = imageFactory;

        this.setPreferredSize(area);
        this.setMinimumSize(area);
        this.setMaximumSize(area);

        this.setBackground(ColorMixerSignleton.drawPanelColor);

        createDrawPaneMouseListeners();

        // set mouse listener
        setMouseListener(Tools.HAND);

        // add key binding for delete
        mainWindow.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("DELETE"), "DELETE");
        mainWindow.getRootPane().getActionMap().put("DELETE", new ActionOnDelete(graph, undoManager, this, mainWindow));

        zoomManager.addObserver(this);
    }

    /**
     * Reaction to notification from zoom manager
     * @param o
     * @param o1 
     */
    @Override
    public void update(Observable o, Object o1) {
        ZoomEventWrapper zoomWrapper = (ZoomEventWrapper) o1;

        //set new sizes of this (JDrawPanel)
        area.width = zoomManager.doScaleToActual(defaultZoomArea.width);
        area.height = zoomManager.doScaleToActual(defaultZoomArea.height);

        this.setSize(area);
        this.setPreferredSize(area);
        this.setMinimumSize(area);
        this.setMaximumSize(area);
        //System.out.println(this.getSize());
    }

    /**
     * Updates size of draw panel after graph change
     */
    public void updateSizeToFitComponents() {
        // find max X and max Y point in components
        int maxX = 0;
        int maxY = 0;

        for (AbstractHwComponent c : graph.getHwComponents()) {
            Point p = c.getLowerRightCornerLocation();
            if (p.x > maxX) {
                maxX = p.x;
            }
            if (p.y > maxY) {
                maxY = p.y;
            }
            //System.out.println("maxx = "+maxX + ", maxy = "+maxY);
        }

        // validate if new size is not smaller than defaultZoomAreaMin
        if (zoomManager.doScaleToDefault(maxX) < defaultZoomAreaMin.getWidth()
                && zoomManager.doScaleToDefault(maxY) < defaultZoomAreaMin.getHeight()) {
            // new size is smaller than defaultZoomAreaMin
            // set default zoom to default zoom min
            defaultZoomArea.setSize(defaultZoomAreaMin.width, defaultZoomAreaMin.height); 
            
            // set area according to defaultZoomArea
            area.setSize(zoomManager.doScaleToActual(defaultZoomArea.width), 
                    zoomManager.doScaleToActual(defaultZoomArea.height));
        } else {
            // update area size
            area.setSize(maxX, maxY);
            // update default zoom size
            defaultZoomArea.setSize(zoomManager.doScaleToDefault(area.width),
                    zoomManager.doScaleToDefault(area.height));
        }

        //System.out.println("area update");


        // let scrool pane in editor know about the change
        this.revalidate();
    }

    /**
     * Updates size of panel according to parameter if rightDownCorner is
     * placed out of panel
     * @param rightDownCorner 
     */
    public void updateSize(Point lowerRightCorner) {
        // make some white space at the edge of panel
        //lowerRightCorner.x += 2;
        //lowerRightCorner.y += 2;

        //System.out.println("Area = "+area);

        // if nothing to resize
        if (!(lowerRightCorner.x > area.width || lowerRightCorner.y > area.height)) {
            return;
        }

        // if lowerRightCorner.x is out of area
        if (lowerRightCorner.x > area.width) {
            // update area width
            area.width = lowerRightCorner.x;
        }

        // if lowerRightCorner.y is out of area
        if (lowerRightCorner.y > area.height) {
            // update area height
            area.height = lowerRightCorner.y;
        }

        // update default zoom size
        defaultZoomArea.setSize(zoomManager.doScaleToDefault(area.width),
                zoomManager.doScaleToDefault(area.height));

        //System.out.println("Area = "+area);

        // let scrool pane in editor know about the change
        this.revalidate();
    }

    /**
     * Sets that cable is being paint
     * @param lineInProgres
     * @param start
     * @param end 
     */
    public void setLineInProgras(boolean lineInProgres, Point start, Point end) {
        this.lineInProgres = lineInProgres;
        lineStart = start;
        lineEnd = end;
    }

    public AbstractImageFactory getImageFactory() {
        return imageFactory;
    }

    /**
     * Creates mouse listeners for all tools
     */
    protected final void createDrawPaneMouseListeners() {
        mouseListenerHand = new DrawPanelListenerStrategyHand(this, undoManager, zoomManager, mainWindow);
        mouseListenerAddHwComponent = new DrawPanelListenerStrategyAddHwComponent(this, undoManager, zoomManager, mainWindow);
        mouseListenerCable = new DrawPanelListenerStrategyAddCable(this, undoManager, zoomManager, mainWindow);
    }

    /**
     * changes mouse listener for DrawPanel according to tool
     * @param tool 
     */
    protected final void setMouseListener(Tools tool) {
        this.removeMouseListener(currentMouseListener);
        this.removeMouseMotionListener(currentMouseListener);
        this.removeMouseWheelListener(currentMouseListener);

        switch (tool) {
            case HAND:
                currentMouseListener = mouseListenerHand;
                break;
            case MAC:
                currentMouseListener = mouseListenerAddHwComponent;
                break;
            case PC:
                currentMouseListener = mouseListenerAddHwComponent;
                break;
            case CABLE:
                currentMouseListener = mouseListenerCable;
                break;
        }

        this.addMouseListener(currentMouseListener);
        this.addMouseMotionListener(currentMouseListener);
        this.addMouseWheelListener(currentMouseListener);
    }

    public Graph getGraph() {
        return graph;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        // set antialiasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // paint line that is being currently made
        if (lineInProgres) {
            g2.drawLine(lineStart.x, lineStart.y, lineEnd.x, lineEnd.y);
        }


        // DRAW cables
        markedCables.clear();
        for (AbstractComponent c : graph.getCables()) {
            if (!c.isMarked()) {
                //g2.draw(c);
                c.paint(g2);
            } else {
                markedCables.add(c);
            }
        }
        for (AbstractComponent c : markedCables) {
            c.paint(g2);
        }


        // DRAW HWcomponents
        markedComponents.clear();
        for (AbstractComponent c : graph.getHwComponents()) {
            if (!c.isMarked()) {
                c.paint(g2);
            } else {
                markedComponents.add(c);
            }
        }
        for (AbstractComponent c : markedComponents) {
            c.paint(g2);
        }
    }

    protected UndoManager getUndoManager() {
        return undoManager;
    }

    protected ZoomManager getZoomManager() {
        return zoomManager;
    }
}
