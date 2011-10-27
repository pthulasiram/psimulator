package psimulator.userInterface.Editor.DrawPanel.UndoCommands;

import javax.swing.undo.AbstractUndoableEdit;
import psimulator.userInterface.Editor.DrawPanel.Components.Cable;
import psimulator.userInterface.Editor.DrawPanel.Graph.GraphInterface;

/**
 *
 * @author Martin
 */
public class UndoableAddCable extends AbstractUndoableEdit {
    protected Cable cable;
    protected GraphInterface graph;
    
    public UndoableAddCable(Cable cable, GraphInterface graph){
        super();
        this.cable = cable;
        this.graph = graph;
    }

    @Override
    public String getPresentationName() {
      return "HW component add/remove";
    }

    @Override
    public void undo() {
      super.undo();
      graph.removeCable(cable);
    }

    @Override
    public void redo() {
      super.redo();
      graph.addCable(cable);
    }
}