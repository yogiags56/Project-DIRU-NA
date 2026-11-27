package MeongDev.DIRUNA.GloftDIRU.core.stage;

import org.junit.Test;
import static org.junit.Assert.*;

public final class WorldStageFixtureManifestTest {
    @Test public void exposesAllAuditedStageDimensions() {
        assertEquals(14,WorldStageFixtureManifest.stageCount(WorldStageFixtureManifest.World.ANGKOR));
        assertEquals(13,WorldStageFixtureManifest.stageCount(WorldStageFixtureManifest.World.SCOTLAND_RESOURCE_BAVARIA_UI));
        assertEquals(14,WorldStageFixtureManifest.stageCount(WorldStageFixtureManifest.World.TIBET_RESOURCE_SIBERIA_UI));
        assertEquals(30,WorldStageFixtureManifest.entry(WorldStageFixtureManifest.World.ANGKOR,5).width());
        assertEquals(75,WorldStageFixtureManifest.entry(WorldStageFixtureManifest.World.ANGKOR,5).height());
        assertEquals(104,WorldStageFixtureManifest.entry(WorldStageFixtureManifest.World.TIBET_RESOURCE_SIBERIA_UI,9).width());
        assertEquals(17,WorldStageFixtureManifest.entry(WorldStageFixtureManifest.World.TIBET_RESOURCE_SIBERIA_UI,9).height());
        int total=0;
        for(WorldStageFixtureManifest.World world:WorldStageFixtureManifest.World.values())
            for(WorldStageFixtureManifest.Entry entry:WorldStageFixtureManifest.entries(world))
                total+=entry.cellsPerLayer();
        assertEquals(50453,total);
    }

    @Test public void rejectsUnknownStageIndicesAndNullWorld() {
        try { WorldStageFixtureManifest.entry(WorldStageFixtureManifest.World.ANGKOR,14);fail(); }
        catch(IndexOutOfBoundsException expected) { }
        try { WorldStageFixtureManifest.stageCount(null);fail(); }
        catch(IllegalArgumentException expected) { }
    }
}
