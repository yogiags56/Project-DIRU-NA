package MeongDev.DIRUNA.GloftDIRU.core.demo;

/** A dry-run callback for one switch label; implementations must not mutate gameplay. */
public interface DemoOpcodeHandler {
    void visit(DemoExecutionContext context, int switchLabel);
}
