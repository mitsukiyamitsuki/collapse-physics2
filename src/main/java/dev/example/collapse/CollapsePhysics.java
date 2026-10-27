package dev.example.collapse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 崩落の物理:
 * 壊したブロックのまわりにある「建材ブロック」のかたまりが、地面などの
 * 地形ブロックにつながっていなければ、かたまりごと落下させる。
 */
public final class CollapsePhysics {
    /** 1つのかたまりの最大ブロック数。これを超えたら「支えられている」とみなす(負荷対策) */
    private static final int MAX_NODES = 600;
    /** 落ちるときの横ぶれの大きさ(0で真下に落ちる) */
    private static final double SCATTER = 0.03;

    /** 落下の対象になる建材(ブロックIDにこの文字が含まれるもの) */
    private static final String[] INCLUDE = {
            "log", "wood", "planks", "stem", "hyphae", "slab", "stairs", "fence", "wall",
            "brick", "concrete", "glass", "wool", "terracotta", "sandstone", "quartz",
            "prismarine", "polished", "smooth_", "cut_", "copper", "bamboo_block", "mosaic"
    };
    /** 建材に見えても対象にしないもの(壁掛けや小物) */
    private static final String[] EXCLUDE = {
            "torch", "sign", "button", "plate", "banner", "head", "hanging", "lantern",
            "candle", "coral", "carpet", "pane", "rail", "lever", "redstone", "ladder", "vine",
            "door", "chest", "bars", "bed", "skull", "frame"
    };

    private CollapsePhysics() {}

    public static void onBlockBroken(ServerLevel level, BlockPos broken) {
        Set<BlockPos> checked = new HashSet<>();
        for (Direction dir : Direction.values()) {
            BlockPos start = broken.relative(dir);
            if (checked.contains(start)) continue;
            if (!isDynamic(level.getBlockState(start))) continue;

            Set<BlockPos> group = new HashSet<>();
            boolean anchored = explore(level, start, group);
            checked.addAll(group);
            if (!anchored) {
                collapse(level, group);
            }
        }
    }

    /** startからつながる建材をたどり、地形に触れていればtrueを返す */
    private static boolean explore(ServerLevel level, BlockPos start, Set<BlockPos> group) {
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        group.add(start);

        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = cur.relative(dir);
                if (group.contains(next)) continue;
                if (!level.hasChunkAt(next)) return true;   // 読み込まれていない所は安全側(落とさない)

                BlockState s = level.getBlockState(next);
                if (isDynamic(s)) {
                    if (group.size() >= MAX_NODES) return true;
                    group.add(next);
                    queue.add(next);
                } else if (isAnchor(level, next, s)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void collapse(ServerLevel level, Set<BlockPos> group) {
        List<BlockPos> list = new ArrayList<>(group);
        list.sort(Comparator.comparingInt(BlockPos::getY));   // 下のブロックから落とす
        for (BlockPos p : list) {
            BlockState s = level.getBlockState(p);
            if (!isDynamic(s)) continue;
            FallingBlockEntity falling = FallingBlockEntity.fall(level, p, s);
            falling.setDeltaMovement(
                    (level.getRandom().nextDouble() - 0.5) * SCATTER,
                    0.0,
                    (level.getRandom().nextDouble() - 0.5) * SCATTER);
        }
    }

    /** 落下の対象になる建材か */
    private static boolean isDynamic(BlockState s) {
        if (s.isAir() || !s.getFluidState().isEmpty() || s.hasBlockEntity()) return false;
        if (s.is(BlockTags.LEAVES)) return false;
        if (s.getBlock() instanceof FallingBlock) return false;   // 砂などは元々落ちる

        String path = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        for (String ex : EXCLUDE) {
            if (path.contains(ex)) return false;
        }
        for (String in : INCLUDE) {
            if (path.contains(in)) return true;
        }
        return false;
    }

    /** 建材を支えてくれる「地形」ブロックか(土・石など、建材ではない中身の詰まったブロック) */
    private static boolean isAnchor(ServerLevel level, BlockPos pos, BlockState s) {
        if (s.isAir() || !s.getFluidState().isEmpty()) return false;
        if (s.is(BlockTags.LEAVES)) return false;
        return s.isCollisionShapeFullBlock(level, pos);
    }
}
