package MeongDev.DIRUNA.GloftDIRU.core.collision;

/** One compare-and-set style grid write requested by a collision decision. */
public final class LegacyCollisionMutationIntent {
    public enum Layer { PLAYER, BACKGROUND, FOREGROUND, PACKED_METADATA, AUXILIARY }
    private final Layer layer;
    private final int x,y,expectedValue,newValue;
    public LegacyCollisionMutationIntent(Layer layer,int x,int y,int expectedValue,int newValue) {
        if(layer==null) throw new IllegalArgumentException("layer is required");
        this.layer=layer;this.x=x;this.y=y;
        this.expectedValue=expectedValue;this.newValue=newValue;
    }
    public Layer getLayer(){return layer;}
    public int getX(){return x;}
    public int getY(){return y;}
    public int getExpectedValue(){return expectedValue;}
    public int getNewValue(){return newValue;}
}
