package psimulator.userInterface.SimulatorEditor.DrawPanel.SwingComponents.InterfacesTable;

import javax.swing.table.AbstractTableModel;
import psimulator.dataLayer.DataLayerFacade;
import psimulator.userInterface.SimulatorEditor.DrawPanel.Components.AbstractHwComponent;
import psimulator.userInterface.SimulatorEditor.DrawPanel.Components.EthInterface;

/**
 *
 * @author Martin Švihlík <svihlma1 at fit.cvut.cz>
 */
public class InterfacesTableModel extends AbstractTableModel {

    private AbstractHwComponent abstractHwComponent;
    private DataLayerFacade dataLayer;
    //
    private boolean showAddresses;
    //
    private String[] columnNames;
    private Object[][] data;// = ...//same as before...

    public InterfacesTableModel(AbstractHwComponent abstractHwComponent, DataLayerFacade dataLayer, boolean showAddresses) {
        this.dataLayer = dataLayer;
        this.abstractHwComponent = abstractHwComponent;
        this.showAddresses = showAddresses;
        
        if(showAddresses){
            String[] names = {dataLayer.getString("INTERFACE"), dataLayer.getString("CONNECTED"),
                dataLayer.getString("CONNECTED_TO"), dataLayer.getString("IP_ADDRESS_MASK"),
                dataLayer.getString("MAC_ADDRESS")};
            columnNames = names;
        }else{
            String[] names = {dataLayer.getString("INTERFACE"), dataLayer.getString("CONNECTED"),
                dataLayer.getString("CONNECTED_TO")};
            columnNames = names;
        }
        
        //

        int interfacesCount = abstractHwComponent.getInterfaceCount();

        data = new Object[interfacesCount][columnNames.length];

        for (int i = 0; i < interfacesCount; i++) {
            EthInterface ethInterface = abstractHwComponent.getEthInterfaceAtIndex(i);

            // fill interface names
            data[i][0] = ethInterface.getName();

            // fill connected status
            data[i][1] = new Boolean(ethInterface.hasCable());

            // fill connected to
            if (ethInterface.hasCable()) {
                if (ethInterface.getCable().getComponent1().getId().intValue() != abstractHwComponent.getId().intValue()) {
                    // set name from component1
                    data[i][2] = ethInterface.getCable().getComponent1().getDeviceName();
                } else {
                    // set name from component2
                    data[i][2] = ethInterface.getCable().getComponent2().getDeviceName();
                }
            } else {
                data[i][2] = "";
            }
            
            
            if(showAddresses){
                // fill IP addresses
                data[i][3] = ethInterface.getIpAddress();

                // fill MAC addresses
                data[i][4] = ethInterface.getMacAddress();
            }
        }
        
        
    }

    @Override
    public int getRowCount() {
        return data.length;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        return data[row][col];
    }

    @Override
    public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        if (col < 3) {
            return false;
        } else {
            return true;
        }
    }

    /*
     * Don't need to implement this method unless your table's data can change.
     */
    @Override
    public void setValueAt(Object value, int row, int col) {
        data[row][col] = value;
        fireTableCellUpdated(row, col);
    }
    
    public boolean hasChangesMade() {
        if (showAddresses) {
            for(int i=0;i<getRowCount();i++){
                // if IP address is different
                if(!abstractHwComponent.getInterfaces().get(i).getIpAddress().equals(getValueAt(i, 3))){
                    return true;
                }
                
                // if MAC address is different
                if(!abstractHwComponent.getInterfaces().get(i).getMacAddress().equals(getValueAt(i, 4))){
                    return true;
                }
                
            }
        }
        return false;
    }
    
    public void copyValuesFromLocalToGlobal() {
        if (showAddresses) {
            for(int i=0;i<getRowCount();i++){
                // save IP
                abstractHwComponent.getInterfaces().get(i).setIpAddress(getValueAt(i,3).toString());
                
                // save MAC
                abstractHwComponent.getInterfaces().get(i).setMacAddress(getValueAt(i,4).toString());
            }
        }
    }
}