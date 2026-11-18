package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyType19SideEffectIntentTest {
    private LegacyType19ScriptScanPlanner.Plan readyPlan() {
        LegacyType19ScriptScanPlanner.Grid grid = new LegacyType19ScriptScanPlanner.Grid() {
            public int width(){return 8;}
            public int height(){return 6;}
            public int playerLayerRawAt(int x,int y){return x==5 && y==2 ? 39 : -1;}
            public int foregroundRawAt(int x,int y){return 0;}
        };
        return LegacyType19ScriptScanPlanner.plan(2,3,19,false,grid);
    }
    @Test public void recordsLegacyStepOrderAndExistingGridState() {
        LegacyType19SideEffectIntent intent = LegacyType19SideEffectIntent.from(readyPlan(),false,true);
        LegacyType19SideEffectIntent.Step[] steps=intent.getSteps();
        assertArrayEquals(new LegacyType19SideEffectIntent.Step[]{
            LegacyType19SideEffectIntent.Step.CONDITIONAL_AR_VOID,
            LegacyType19SideEffectIntent.Step.SET_L_TRUE,
            LegacyType19SideEffectIntent.Step.SET_AL_FALSE,
            LegacyType19SideEffectIntent.Step.ENSURE_AUXILIARY_GRID,
            LegacyType19SideEffectIntent.Step.ALLOCATE_SLOT_FROM_ANCHOR,
            LegacyType19SideEffectIntent.Step.INITIALIZE_M_SLOT,
            LegacyType19SideEffectIntent.Step.SCAN_AND_CLEAR_FOREGROUND_RUN
        },steps);
        assertTrue(intent.isArVoidCalled());
        assertFalse(intent.doesArVoidResetArrays());
        assertFalse(intent.shouldAllocateAuxiliaryGrid());
        assertEquals(5,intent.getAnchorX());
        assertEquals(2,intent.getAnchorY());
    }
    @Test public void skipsArVoidWhenLegacyFlagAlreadyTrue() {
        LegacyType19SideEffectIntent intent = LegacyType19SideEffectIntent.from(readyPlan(),true,false);
        assertFalse(intent.isArVoidCalled());
        assertFalse(intent.doesArVoidResetArrays());
        assertTrue(intent.shouldAllocateAuxiliaryGrid());
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsMissingPlan() {
        LegacyType19SideEffectIntent.from(null,false,false);
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsIncompleteScanPlan() {
        LegacyType19ScriptScanPlanner.Grid grid = new LegacyType19ScriptScanPlanner.Grid() {
            public int width(){return 8;}
            public int height(){return 6;}
            public int playerLayerRawAt(int x,int y){return -1;}
            public int foregroundRawAt(int x,int y){return 0;}
        };
        LegacyType19SideEffectIntent.from(
            LegacyType19ScriptScanPlanner.plan(2,3,19,false,grid),false,false);
    }
}