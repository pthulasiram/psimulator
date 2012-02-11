package psimulator.userInterface.SimulatorEditor.DrawPanel.MouseActionListeners;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoManager;
import psimulator.dataLayer.DataLayerFacade;
import psimulator.userInterface.MainWindowInnerInterface;
import psimulator.userInterface.SimulatorEditor.DrawPanel.Components.AbstractHwComponent;
import psimulator.userInterface.SimulatorEditor.DrawPanel.Components.Cable;
import psimulator.userInterface.SimulatorEditor.DrawPanel.Components.EthInterface;
import psimulator.userInterface.SimulatorEditor.DrawPanel.DrawPanelInnerInterface;
import psimulator.userInterface.SimulatorEditor.DrawPanel.SwingComponents.CableConnectToInterfacePopupMenu;
import psimulator.userInterface.SimulatorEditor.DrawPanel.UndoCommands.UndoableAddCable;
import psimulator.userInterface.SimulatorEditor.DrawPanel.ZoomManager;
import psimulator.userInterface.SimulatorEditor.Tools.AbstractTool;
import psimulator.userInterface.SimulatorEditor.Tools.CreateCableTool;

/**
 *
 * @author Martin
 */
public class DrawPanelListenerStrategyAddCable extends DrawPanelListenerStrategy implements ChooseEthInterfaceInterface {

    private AbstractHwComponent component1;
    private AbstractHwComponent component2;
    private EthInterface eth1;
    private EthInterface eth2;
    boolean hasFirstComponent = false;
    boolean hasSecondComponent = false;
    private Point startPointInDefault;
    private CableConnectToInterfacePopupMenu popupMenu;
    
    private CreateCableTool createCableTool;

    public DrawPanelListenerStrategyAddCable(DrawPanelInnerInterface drawPanel, UndoManager undoManager, ZoomManager zoomManager, MainWindowInnerInterface mainWindow, DataLayerFacade dataLayer) {
        super(drawPanel, undoManager, zoomManager, mainWindow, dataLayer);

        popupMenu = new CableConnectToInterfacePopupMenu(drawPanel, new PopupInterfaceChooseListener(), this);
    }

    @Override
    public void initialize() {
    }
    
    /**
     * sets cursor to default and inits cable making to start
     */
    @Override
    public void deInitialize() {
        //drawPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        initVariablesForCableMaking();
        drawPanel.repaint();
    }
    
    @Override
    public void setTool(AbstractTool tool) {
        this.createCableTool = (CreateCableTool) tool;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (hasFirstComponent) {
            /*
            drawPanel.setLineInProgras(true, startPointInDefault, 
                    zoomManager.doScaleToDefault(e.getPoint()));*/

            drawPanel.setLineInProgras(true, startPointInDefault, 
                    e.getPoint());
            
            drawPanel.repaint();
        }

        // if mouse over any HW component
        for (AbstractHwComponent c : drawPanel.getGraphOuterInterface().getHwComponents()) {
            if (c.intersects(e.getPoint())) {
                // change cursor
                drawPanel.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
                return;
            }
        }

        drawPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * reactin to mouse click or press
     * @param e 
     */
    @Override
    public void mousePressedLeft(MouseEvent e) {

        // if popup shown, no pressing on other components
        /*
        if (popupMenu.isVisible()) {
            System.out.println("No pressing");
            return;
        }*/

        // clicked component
        AbstractHwComponent tmp = null;

        /*
        // find if any component clicked
        for (AbstractHwComponent c : graph.getHwComponents()) {
            if (c.intersects(e.getPoint())) {
                tmp = c;
            }
        }*/
        
        tmp = getClickedAbstractHwComponent(e.getPoint());

        // if nothing clicked
        if (tmp == null) {
            initVariablesForCableMaking();
            drawPanel.repaint();
            return;
        }

        // if we clicked first component
        if (hasFirstComponent == false) {
            // set first component
            component1 = tmp;
            hasFirstComponent = true;
            // set start point
            //startPointInDefault = zoomManager.doScaleToDefault(component1.getCenterLocation());
            startPointInDefault = component1.getCenterLocationDefaultZoom();
        } else {
            // if we clicked second component
            component2 = tmp;
            hasSecondComponent = true;
        }

        // if we have clicked first component
        if (hasFirstComponent && !hasSecondComponent) {
            // if first component dont have any free interface
            if (!component1.hasFreeInterace()) {
                // show meesage dialog
                doShowInformMessageDialog(dataLayer.getString("NO_INTERFACE_AVAIABLE"), dataLayer.getString("CONNECTION_PROBLEM"));
                // cancel cable making
                initVariablesForCableMaking();
            } else {
                // if component has free interface
                doChooseInterface(e, component1);
            }
        }

        // if we have both components
        if (hasFirstComponent && hasSecondComponent) {
            if (component1 == component2) {
                // show meesage dialog
                doShowInformMessageDialog(dataLayer.getString("CANT_CONNECT_TO_ITSELF"), dataLayer.getString("CONNECTION_PROBLEM"));
                
            } else {
                // if second component dont have any free interface
                if (!component2.hasFreeInterace()) {
                    // show meesage dialog
                    doShowInformMessageDialog(dataLayer.getString("NO_INTERFACE_AVAIABLE"), dataLayer.getString("CONNECTION_PROBLEM"));
                    // remove second component from cable making
                    removeSecondComponentFromCable();
                } else {
                    // if component has free interface
                    doChooseInterface(e, component2);
                }
            }
        }
    }
   
    @Override
    public void mousePressedRight(MouseEvent e) {
        AbstractHwComponent tmp = getClickedAbstractHwComponent(e.getPoint());
        // if something clicked, or has first component
        if(tmp!= null || hasFirstComponent){
            mousePressedLeft(e);
        }else{ // if nothing clicked
            drawPanel.doSetDefaultToolInEditorToolBar();
        }
        
    }
    
    /**
     * sets chosen interface to component accornig to actual cable making progres.
     * @param ethInterface 
     */
    @Override
    public void setChosenInterface(EthInterface ethInterface) {
        // if we have only first component
        if (hasFirstComponent && !hasSecondComponent) {
            // get choosen ethInterface
            eth1 = ethInterface;
            //System.out.println("chosen interface1" + eth1);
        }

        // if we have first and second component
        if (hasFirstComponent && hasSecondComponent) {
            // get choosen ethInterface
            eth2 = ethInterface;
            //System.out.println("chosen interface2" + eth2);
            // connect components
            connectComponents(component1, component2, eth1, eth2);
        }

    }

    
    /**
     * Shows inform message dialog with message and title
     * @param message
     * @param title 
     */
    private void doShowInformMessageDialog(String message, String title){
        JOptionPane.showMessageDialog(mainWindow.getRootPane(), message, title, JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Chooses interface. If right mouse button clicked, than user sets which interface to use, if left
     * button clicked, than it uses first avaiable interface
     * @param e Mouse event
     * @param component Component that is beeing connected
     */
    private void doChooseInterface(MouseEvent e, AbstractHwComponent component) {
        // if right mouse button clicked
        if (SwingUtilities.isRightMouseButton(e)) {
            // show popup menu with choose
            popupMenu.showPopupInterfaceChoose(component, e.getX(), e.getY());
        } else { // left button clicked
            // choose interface automaticly
            setChosenInterface(component.getFirstFreeInterface());
        }
    }
    
    
    /**
     * connects components c1 and c2 on EthInterfaces eth1 and eth2 in Graph
     * @param c1
     * @param c2
     * @param eth1
     * @param eth2 
     */
    private void connectComponents(AbstractHwComponent c1, AbstractHwComponent c2, EthInterface eth1, EthInterface eth2) {
        // create new cabel
        Cable cable = new Cable(dataLayer, drawPanel.getImageFactory(), zoomManager, createCableTool.getHwType(), c1, c2, eth1, eth2);
        
        // initialize cable
        cable.initialize();
        
        // add cabel to graph
        drawPanel.getGraphOuterInterface().addCable(cable);
        
        // add to undo manager
        undoManager.undoableEditHappened(new UndoableEditEvent(this,
                new UndoableAddCable(cable, drawPanel.getGraphOuterInterface())));

        initVariablesForCableMaking();

        // repaint draw panel
        drawPanel.repaint();
    }

    /**
     * inits variables for cable making and sets line in progres to false
     */
    private void initVariablesForCableMaking() {
        component1 = null;
        component2 = null;
        eth1 = null;
        eth2 = null;
        hasFirstComponent = false;
        hasSecondComponent = false;
        drawPanel.setLineInProgras(false, null, null);
    }

    /**
     * removes second component from cable making
     */
    private void removeSecondComponentFromCable() {
        component2 = null;
        eth2 = null;
        hasSecondComponent = false;
    }


    /**
     * PopupMenuListener to handle events on popup
     */
    class PopupInterfaceChooseListener implements PopupMenuListener {

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            // not used
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            // not used
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
            //System.out.println("Popup canceled!");
            // if canceled on first component
            if (!hasSecondComponent) {
                // remove cable and init
                initVariablesForCableMaking();
            } else {
                // if canceled on second component
                // remove second component from cable
                removeSecondComponentFromCable();
            }

            drawPanel.repaint();
        }
    }
}