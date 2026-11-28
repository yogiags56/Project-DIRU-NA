package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Typed intent for player-local, animation, movement-flag, or foreground-cache state.
 * This is a proposal only; it never writes gameplay state.
 */
public final class LegacyCollisionStateIntent {
    public enum Channel { PLAYER, ANIMATION, MOVEMENT_FLAGS, FOREGROUND_CACHE, B_BYTE_STATE, C_BYTE_STATE }
    public enum Field {
        TILE_X, TILE_Y, PENDING_DIRECTION, ACTIVE_DIRECTION, INTERPOLATION_PROGRESS,
        ANIMATION_ID, ANIMATION_FRAME, BLOCKED_FLAG, CACHE_VALUE, BYTE_VALUE
    }

    private final Channel channel;
    private final Field field;
    private final boolean coordinateScoped;
    private final int x, y, expectedValue, newValue;

    private LegacyCollisionStateIntent(Channel channel, Field field, boolean coordinateScoped,
                                       int x, int y, int expectedValue, int newValue) {
        if (channel == null || field == null) throw new IllegalArgumentException("channel and field are required");
        validatePair(channel, field, coordinateScoped);
        this.channel=channel; this.field=field; this.coordinateScoped=coordinateScoped;
        this.x=x; this.y=y; this.expectedValue=expectedValue; this.newValue=newValue;
    }

    public static LegacyCollisionStateIntent scalar(Channel channel, Field field,
                                                     int expectedValue, int newValue) {
        return new LegacyCollisionStateIntent(channel,field,false,0,0,expectedValue,newValue);
    }

    public static LegacyCollisionStateIntent cell(Channel channel, Field field,
                                                   int x, int y, int expectedValue, int newValue) {
        return new LegacyCollisionStateIntent(channel,field,true,x,y,expectedValue,newValue);
    }

    private static void validatePair(Channel channel, Field field, boolean coordinateScoped) {
        boolean valid;
        switch(channel) {
            case PLAYER:
                valid = field==Field.TILE_X || field==Field.TILE_Y
                        || field==Field.PENDING_DIRECTION || field==Field.ACTIVE_DIRECTION
                        || field==Field.INTERPOLATION_PROGRESS;
                break;
            case ANIMATION:
                valid = field==Field.ANIMATION_ID || field==Field.ANIMATION_FRAME;
                break;
            case MOVEMENT_FLAGS:
                valid = field==Field.BLOCKED_FLAG;
                break;
            case FOREGROUND_CACHE:
                valid = field==Field.CACHE_VALUE && coordinateScoped;
                break;
            case B_BYTE_STATE:
            case C_BYTE_STATE:
                valid = field==Field.BYTE_VALUE && coordinateScoped;
                break;
            default: valid=false;
        }
        if (!valid || (coordinateScoped && channel!=Channel.FOREGROUND_CACHE
                && channel!=Channel.B_BYTE_STATE && channel!=Channel.C_BYTE_STATE))
            throw new IllegalArgumentException("field is not valid for state channel");
    }

    public Channel getChannel(){return channel;}
    public Field getField(){return field;}
    public boolean isCoordinateScoped(){return coordinateScoped;}
    public int getX(){return x;}
    public int getY(){return y;}
    public int getExpectedValue(){return expectedValue;}
    public int getNewValue(){return newValue;}
}
