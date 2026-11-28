package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Pure decision model for player-layer 0/8/9/47.
 * The caller supplies facts sampled from the legacy grid and the already
 * evaluated kByte==4 early-return condition. This class never reads or writes
 * a grid and never performs a push.
 */
public final class LegacyTwoCellMovementEvaluator {
    public static final class Decision {
        private final boolean earlyReturn;
        private final boolean returnFlag;
        private final int counterAfter;
        private final int earlyReturnAnimation;
        private final boolean pushAccepted;
        private final boolean requestWorldPush;
        private final boolean setMovementBlock;
        private final boolean clearLocalDirection;
        private final boolean clearPlayerMetadataLowBits;
        private final boolean pushTowardPositiveDirection;

        private Decision(boolean earlyReturn, boolean returnFlag, int counterAfter,
                         int earlyReturnAnimation,
                         boolean pushAccepted, boolean requestWorldPush,
                         boolean setMovementBlock, boolean clearLocalDirection,
                         boolean clearPlayerMetadataLowBits,
                         boolean pushTowardPositiveDirection) {
            this.earlyReturn=earlyReturn;
            this.returnFlag=returnFlag;
            this.counterAfter=counterAfter;
            this.earlyReturnAnimation=earlyReturnAnimation;
            this.pushAccepted=pushAccepted;
            this.requestWorldPush=requestWorldPush;
            this.setMovementBlock=setMovementBlock;
            this.clearLocalDirection=clearLocalDirection;
            this.clearPlayerMetadataLowBits=clearPlayerMetadataLowBits;
            this.pushTowardPositiveDirection=pushTowardPositiveDirection;
        }
        public boolean isEarlyReturn(){return earlyReturn;}
        public boolean getReturnFlag(){return returnFlag;}
        public int getCounterAfter(){return counterAfter;}
        /** 8 or 9 for the legacy blocked-movement animation; zero otherwise. */
        public int getEarlyReturnAnimation(){return earlyReturnAnimation;}
        public boolean isPushAccepted(){return pushAccepted;}
        public boolean isWorldPushRequested(){return requestWorldPush;}
        public boolean shouldSetMovementBlock(){return setMovementBlock;}
        public boolean shouldClearLocalDirection(){return clearLocalDirection;}
        public boolean shouldClearPlayerMetadataLowBits(){return clearPlayerMetadataLowBits;}
        public boolean isPushTowardPositiveDirection(){return pushTowardPositiveDirection;}
    }

    private LegacyTwoCellMovementEvaluator(){}

    public static Decision evaluate(boolean earlyReturnConditionMet,
                                    int incomingCounter,
                                    boolean probeMode,
                                    boolean directionNonZero,
                                    boolean positiveDirection,
                                    boolean destinationPassable,
                                    int targetForegroundLowByte,
                                    int targetForegroundSignedHighByte,
                                    int adjacentPlayerLayer,
                                    int adjacentForegroundLowByte,
                                    int movementStateBits, boolean incomingReturnFlag) {
        if(earlyReturnConditionMet) {
            // Legacy returns false before decrementing the movement counter.
            int animation=(((movementStateBits | 0x8) & 0x7)==0x2) ? 8 : 9;
            return new Decision(true,false,incomingCounter,animation,false,false,
                    false,false,false,positiveDirection);
        }

        int counterAfter=incomingCounter-1;
        boolean counterAllowsPush=counterAfter<0 || probeMode;
        boolean foregroundAllowsPush=targetForegroundLowByte!=7
                || (targetForegroundSignedHighByte & 0xF0)!=0;
        boolean adjacentPlayerAllowsPush=
                (adjacentPlayerLayer!=19 && adjacentPlayerLayer!=45
                        && adjacentPlayerLayer!=49 && adjacentPlayerLayer!=43)
                || adjacentForegroundLowByte==0x23;

        boolean accepted=counterAllowsPush && directionNonZero
                && destinationPassable && foregroundAllowsPush
                && adjacentPlayerAllowsPush;

        if(accepted) {
            return new Decision(false,true,counterAfter,0,true,!probeMode,
                    true,false,false,positiveDirection);
        }

        return new Decision(false,incomingReturnFlag,counterAfter,0,false,false,
                directionNonZero,true,true,positiveDirection);
    }
    /**
     * Evaluates the two-cell branch directly from an immutable, named snapshot.
     * The snapshot must already contain samples from the exact source cells.
     */
    public static Decision evaluate(LegacyTwoCellMovementSnapshot snapshot) {
        if(snapshot == null) throw new IllegalArgumentException("snapshot");
        return evaluate(snapshot.isEarlyReturnConditionMet(),
                snapshot.getMovementCounter(),
                snapshot.isProbeMode(),
                snapshot.getDeltaX() != 0,
                snapshot.getDeltaX() > 0,
                snapshot.isDestinationPassable(),
                snapshot.getTargetForegroundLowByte(),
                snapshot.getTargetForegroundSignedHighByte(),
                snapshot.getAdjacentPlayerLayer(),
                snapshot.getAdjacentForeground() & 0xFF,
                snapshot.getResolvedMovementStateBits(),
                snapshot.getIncomingReturnFlag());
    }

}
