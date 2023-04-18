package io.github.opencubicchunks.cubicchunks.levelgen.lighting;

import java.util.function.BiFunction;
import java.util.function.Function;

import io.github.opencubicchunks.cubicchunks.mock.TestBlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LayerLightEngine;
import org.jetbrains.annotations.NotNull;

public class LightSlice {
    private static final String XZ_SLICE_GAP = " ".repeat(10);
    private static final String LEFT_MARGIN_GAP = " ".repeat(8);
    private static final char[] HEX_CHARS = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    public static StringBuilder createXZLightSlices(LayerLightEngine<?, ?> lightEngine, TestBlockGetter blockGetter,
                                             int errorX, int errorY, int errorZ,
                                             int startX, int endX, int startY, int endY, int startZ, int endZ) {

        return createXZLightSlices(lightEngine, pos -> {
            BlockState blockState = blockGetter.getNullableBlockState(pos);
            if (blockState == null) {
                return BlockOpacityState.UNLOADED;
            }
            return blockState.canOcclude() ? BlockOpacityState.OPAQUE : BlockOpacityState.TRANSPARENT;
        }, errorX, errorY, errorZ, startX, endX, startY, endY, startZ, endZ);
    }

    private static StringBuilder createXZLightSlices(LayerLightEngine<?, ?> lightEngine, Function<BlockPos, BlockOpacityState> opaqueState,
                                             int errorX, int errorY, int errorZ,
                                             int startX, int endX, int startY, int endY, int startZ, int endZ) {
        StringBuilder stringBuilder = new StringBuilder();

        appendHeader(errorX, errorZ, stringBuilder, endX - startX, endZ - startZ);

        appendBody(lightEngine, opaqueState, errorX, errorY, errorZ, startX, endX, startY, endY, startZ, endZ, stringBuilder);

        appendFooter(stringBuilder, startX, endX, startZ, endZ);

        return stringBuilder;
    }

    private static void appendHeader(int errorX, int errorZ, StringBuilder stringBuilder, int xRange, int zRange) {
        stringBuilder.append(LEFT_MARGIN_GAP)
            .append(String.format("%-" + (2 * xRange) + "s", "XY Slice " + errorZ))
            .append(XZ_SLICE_GAP)
            .append(String.format("%-" + (2 * zRange) + "s", "ZY Slice " + errorX))
            .append("\n");
    }

    private static void appendBody(LayerLightEngine<?, ?> lightEngine, Function<BlockPos, BlockOpacityState> opaqueState,
                                   int errorX, int errorY, int errorZ,
                                   int startX, int endX, int startY, int endY, int startZ, int endZ,
                                   StringBuilder stringBuilder) {
        for (int y = endY - 1; y >= startY; y--) {
            stringBuilder.append(String.format("%4d >  ", y));
            appendRow(stringBuilder, y, startX, endX, errorX, errorY, (row, col) -> {
                BlockPos pos = new BlockPos(row, col, errorZ);
                return getLightCharacterForPos(lightEngine, opaqueState, pos);
            });

            stringBuilder.append(XZ_SLICE_GAP);
            appendRow(stringBuilder, y, startZ, endZ, errorZ, errorY, (row, col) -> {
                BlockPos pos = new BlockPos(errorX, col, row);
                return getLightCharacterForPos(lightEngine, opaqueState, pos);
            });

            stringBuilder.append("\n");
        }
    }

    private static void appendRow(StringBuilder sb, int y, int start, int end, int errorRow, int errorColumn,
                                  BiFunction<Integer, Integer, Character> rowColToLightValue) {
        for (int i = start; i < end; i++) {
            if (i == errorRow && y == errorColumn) {
                sb.deleteCharAt(sb.length() - 1);
                sb.append(">");
            }
            sb.append(rowColToLightValue.apply(i, y));
            sb.append((i == errorRow && y == errorColumn) ? "<" : " ");
        }
    }

    private static void appendFooter(StringBuilder stringBuilder, int startX, int endX, int startZ, int endZ) {
        stringBuilder
            .append("\n").append(LEFT_MARGIN_GAP)
            .append("^ ".repeat(Math.max(0, endX - startX)))
            .append(XZ_SLICE_GAP)
            .append("^ ".repeat(Math.max(0, endZ - startZ)))
            .append("\n").append(LEFT_MARGIN_GAP);

        for (int x = startX; x < endX; x++) {
            stringBuilder.append(String.format("%-2d", x));
        }
        stringBuilder.append(XZ_SLICE_GAP);
        for (int z = startZ; z < endZ; z++) {
            stringBuilder.append(String.format("%-2d", z));
        }
        stringBuilder.append("\n");
    }

    @NotNull private static Character getLightCharacterForPos(LayerLightEngine<?, ?> lightEngine, Function<BlockPos, BlockOpacityState> opaqueState, BlockPos pos) {
        int light = lightEngine.getLightValue(pos);
        if (light == 0) {
            BlockOpacityState opacityState = opaqueState.apply(pos);
            if (opacityState == BlockOpacityState.UNLOADED) {
                return '.';
            }
            if (opacityState == BlockOpacityState.OPAQUE) {
                return '#';
            }
        }
        return HEX_CHARS[light];
    }

    public enum BlockOpacityState {
        OPAQUE,
        TRANSPARENT,
        UNLOADED,
    }
}