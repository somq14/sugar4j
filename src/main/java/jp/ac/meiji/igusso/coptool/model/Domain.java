package jp.ac.meiji.igusso.coptool.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

@EqualsAndHashCode
public final class Domain implements Iterable<Integer> {
  @Getter private final List<Integer> values;

  private Domain(@NonNull Collection<Integer> values) {
    List<Integer> copy = new ArrayList<>(new TreeSet<>(values));
    Collections.sort(copy);
    this.values = Collections.unmodifiableList(copy);
  }

  public int size() {
    return values.size();
  }

  public int get(int index) {
    return values.get(index);
  }

  public boolean contains(int value) {
    int index = Collections.binarySearch(values, value);
    return index < values.size() && values.get(index) == value;
  }

  @Override
  public Iterator<Integer> iterator() {
    return values.iterator();
  }

  @Override
  public String toString() {
    List<String> seqs = new ArrayList<>();
    int seqBegin = 0;
    while (seqBegin < values.size()) {
      int seqEnd = seqBegin;
      while (seqEnd + 1 < values.size() && values.get(seqEnd) + 1 == values.get(seqEnd + 1)) {
        seqEnd++;
      }

      if (seqEnd - seqBegin + 1 <= 3) {
        for (int i = seqBegin; i <= seqEnd; i++) {
          seqs.add(String.valueOf(values.get(i)));
        }
      } else {
        seqs.add(String.format("[%d, %d]", values.get(seqBegin), values.get(seqEnd)));
      }
      seqBegin = seqEnd + 1;
    }

    return String.format("Domain(%s)", String.join(", ", seqs));
  }

  public static Domain of(@NonNull Collection<Integer> values) {
    return new Domain(values);
  }

  public static Domain of(int lowerBound, int upperBound) {
    if (lowerBound > upperBound) {
      throw new IllegalArgumentException(
          "Empty Bound Is Not Allowed: [" + lowerBound + ", " + upperBound + "]");
    }

    List<Integer> values = new ArrayList<>();
    for (int i = lowerBound; i <= upperBound; i++) {
      values.add(i);
    }
    return new Domain(values);
  }

  public static Domain of(int size) {
    return of(0, size - 1);
  }
}
