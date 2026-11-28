package MeongDev.DIRUNA.GloftDIRU.core.demo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registry of explicitly audited opcode entry points for dry-run dispatch. */
public final class DemoOpcodeHandlerRegistry {
    private final Map<Integer, DemoOpcodeHandler> handlers;

    private DemoOpcodeHandlerRegistry(Map<Integer, DemoOpcodeHandler> handlers) {
        this.handlers = Collections.unmodifiableMap(new LinkedHashMap<Integer, DemoOpcodeHandler>(handlers));
    }

    public static DemoOpcodeHandlerRegistry dryRunRegistry() {
        Map<Integer, DemoOpcodeHandler> map = new LinkedHashMap<Integer, DemoOpcodeHandler>();
        DemoOpcodeHandler traceOnly = new DemoOpcodeHandler() {
            @Override public void visit(DemoExecutionContext context, int switchLabel) {
                context.visitSwitchLabel(switchLabel);
            }
        };
        for (Integer opcode : DemoOpcodeOperandDescriptor.all().keySet()) {
            if (hasDispatchPlan(opcode)) map.put(opcode, traceOnly);
        }
        return new DemoOpcodeHandlerRegistry(map);
    }

    public DemoOpcodeHandler handlerFor(int opcode) { return handlers.get(opcode); }
    public boolean supports(int opcode) { return handlers.containsKey(opcode); }
    public Map<Integer, DemoOpcodeHandler> handlers() { return handlers; }

    private static boolean hasDispatchPlan(int opcode) {
        try {
            DemoOpcodeDispatchPlan.forOpcode(opcode, false);
            return true;
        } catch (IllegalArgumentException unsupported) {
            return false;
        }
    }
}
