package de.uni_hannover.sra.minimax_simulator.model.machine.minimax.group;

import de.uni_hannover.sra.minimax_simulator.model.machine.base.display.FontMetricsProvider;
import de.uni_hannover.sra.minimax_simulator.model.machine.base.topology.MachineTopology;
import de.uni_hannover.sra.minimax_simulator.model.machine.minimax.Parts;
import de.uni_hannover.sra.minimax_simulator.model.machine.minimax.layout.DefaultRegisterLayoutSet;
import de.uni_hannover.sra.minimax_simulator.model.machine.minimax.layout.LayoutSet;
import de.uni_hannover.sra.minimax_simulator.model.machine.part.*;
import de.uni_hannover.sra.minimax_simulator.model.machine.shape.LabelShape;

/**
 * Groups the default components of a register.
 *
 * @author Martin L&uuml;ck
 */
public class DefaultRegisterGroup extends AbstractGroup {

    private final String registerId;

    /**
     * Constructs a new {@code DefaultRegisterGroup} for the specified register.
     *
     * @param registerId
     *          the ID of the register
     */
    public DefaultRegisterGroup(String registerId) {
        this.registerId = registerId;
    }

    @Override
    public void initialize(MachineTopology cr, FontMetricsProvider fontProvider) {
        Register register = cr.getCircuit(Register.class, registerId);

        Junction junction = new Junction();
        junction.getDataOuts().add(new OutgoingPin(junction));
        // For now, don't chain the register junctions, but make a new cable to each of them
        // just create an empty port for the looks
        junction.getDataOuts().add(new OutgoingPin(junction));

        Label label = new Label(register.getLabel() + ".W");
        label.setShape(new LabelShape(fontProvider));

        Port port = new Port(register.getLabel() + ".W");

        Wire aluWire = new Wire(3, cr.getCircuit(Alu.class, Parts.ALU).getOutData(), junction.getDataIn());
        Wire dataInWire = new Wire(2, junction.getDataOuts().get(0), register.getDataIn());
        Wire enabledWire = new Wire(2, port.getDataOut(), register.getWriteEnabled());

        add(junction, registerId + Parts._JUNCTION);
        add(label, registerId + Parts._LABEL);
        add(port, registerId + Parts._PORT);

        addWire(aluWire, registerId + Parts._JUNCTION + Parts._WIRE_DATA_IN);
        addWire(dataInWire, registerId + Parts._WIRE_DATA_IN);
        addWire(enabledWire, registerId + Parts._WIRE_ENABLED);
    }

    @Override
    public boolean hasLayouts() {
        return true;
    }

    @Override
    public LayoutSet createLayouts() {
        return new DefaultRegisterLayoutSet(registerId);
    }
}