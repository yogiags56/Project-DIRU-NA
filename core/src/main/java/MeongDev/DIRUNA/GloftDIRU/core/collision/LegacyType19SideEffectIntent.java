package MeongDev.DIRUNA.GloftDIRU.core.collision;

/** Immutable description of the legacy type-19 side-effect sequence. */
public final class LegacyType19SideEffectIntent {
    public enum Step { CONDITIONAL_AR_VOID, SET_L_TRUE, SET_AL_FALSE, ENSURE_AUXILIARY_GRID, ALLOCATE_SLOT_FROM_ANCHOR, INITIALIZE_M_SLOT, SCAN_AND_CLEAR_FOREGROUND_RUN }
    private final Step[] steps;
    private final boolean arVoidCalled;
    private final boolean arVoidResetsArrays;
    private final boolean allocateAuxiliaryGrid;
    private final int anchorX, anchorY;
    private LegacyType19SideEffectIntent(Step[] steps, boolean called, boolean resets, boolean alloc, int x, int y) {
        this.steps=steps.clone(); arVoidCalled=called; arVoidResetsArrays=resets;
        allocateAuxiliaryGrid=alloc; anchorX=x; anchorY=y;
    }
    public Step[] getSteps(){return steps.clone();}
    public boolean isArVoidCalled(){return arVoidCalled;}
    public boolean doesArVoidResetArrays(){return arVoidResetsArrays;}
    public boolean shouldAllocateAuxiliaryGrid(){return allocateAuxiliaryGrid;}
    public int getAnchorX(){return anchorX;}
    public int getAnchorY(){return anchorY;}
    public static LegacyType19SideEffectIntent from(LegacyType19ScriptScanPlanner.Plan plan, boolean lBooleanBefore, boolean auxiliaryGridExists) {
        if (plan == null || !plan.isReady()) throw new IllegalArgumentException("ready scan plan required");
        boolean callAr = !lBooleanBefore;
        boolean resetArrays = callAr && lBooleanBefore;
        boolean allocate = !auxiliaryGridExists;
        return new LegacyType19SideEffectIntent(new Step[]{
            Step.CONDITIONAL_AR_VOID, Step.SET_L_TRUE, Step.SET_AL_FALSE,
            Step.ENSURE_AUXILIARY_GRID, Step.ALLOCATE_SLOT_FROM_ANCHOR,
            Step.INITIALIZE_M_SLOT, Step.SCAN_AND_CLEAR_FOREGROUND_RUN
        }, callAr, resetArrays, allocate, plan.getAnchorX(), plan.getAnchorY());
    }
}