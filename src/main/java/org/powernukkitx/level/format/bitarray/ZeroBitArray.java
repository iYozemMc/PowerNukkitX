package org.powernukkitx.level.format.bitarray;

import org.powernukkitx.math.NukkitMath;

/**
 * @author iYozemMc
 */

public record ZeroBitArray(BitArrayVersion version, int size) implements BitArray {
    public ZeroBitArray {
        if (version == BitArrayVersion.V0){
            throw new IllegalArgumentException("V0 is stored as a SingletonBitArray");
        }
    }

    @Override
    public void set(int index, int value) {
        if ((value & this.version.maxEntryValue) != 0){
            throw new UnsupportedOperationException("A ZeroBitArray only holds 0, replace it before storing " + value);
        }
    }

    @Override
    public int get(int index) {
        return 0;
    }

    public int wordCount() {
        return NukkitMath.ceilFloat((float) this.size / this.version.entriesPerWord);
    }

    @Override
    public int[] words() {
        return new int[this.wordCount()];
    }

    @Override
    public BitArray copy() {
        return this;
    }
}
