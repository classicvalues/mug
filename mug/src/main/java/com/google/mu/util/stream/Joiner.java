package com.google.mu.util.stream;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * An joiner of strings.
 *
 * <p>When used to join a pair of objects (for example in a BiStream), it can be used for BiFunction:
 * <pre>{@code
 *   BiStream.zip(userIds, names)
 *       .mapToObj(Joiner.on('=')::join)  // (id, name) -> id + "=" + name
 *       ....;
 * }</pre>
 *
 * <p>Alternatively, it can be used as a Collector. For example,
 * {@code names.collect(Joiner.on(','))} is equivalent to {@code names.collect(Collectors.joining(","))}.
 *
 * Except that JDK {@code joining()} requires the inputs to be strings; while Joiner can join any input,
 * e.g. numbers.
 *
 * <p>You can also chain {@link #between} to further enclose the joined result.
 * The following code joins a list of ids and their corresponding names in the format of
 * {@code "[id1:name1, id2:name2, id3:name3, ...]"}:
 * <pre>{@code
 *   BiStream.zip(userIds, names)
 *       .mapToObj(Joiner.on('=')::join)
 *       .collect(Joiner.on(", ").between('[', ']'));
 * }</pre>
 * Which reads clearer than using JDK {@link Collectors#joining}:
 * <pre>{@code
 *   BiStream.zip(userIds, names)
 *       .mapToObj(id, name) -> id + ":" + name)
 *       .collect(Collectors.joining(", ", "[", "]"));
 * }</pre>
 *
 * <p>Or, you can format the {@code (id, name)} pairs in the format of
 * {@code "[(id1, name1), (id2, name2). ...]":
 * <pre>{@code
 *   BiStream.zip(userIds, names)
 *       .mapToObj(Joiner.on(", ").between('(', ')')::join)
 *       .collect(Joiner.on(", ").between('[', ']'));
 * }</pre>
 *
 * <p>Unlike {@link com.google.common.base.Joiner}, nulls are forgiven and will be stringified
 * using {@link String#valueOf}. In addition, {@link #between} can be chained to further enclose
 * the joined result.
 *
 * @since 5.6
 */
public abstract class Joiner implements Collector<Object, StringJoiner, String> {

  /** Joining the components on the {@code delimiter} character */
  public static Joiner on(char delimiter) {
    return on(Character.toString(delimiter));
  }

  /** Joining the components on the {@code delimiter} string */
  public static Joiner on(String delimiter) {
    requireNonNull(delimiter);
    return new Joiner() {
      @Override
      public Supplier<StringJoiner> supplier() {
        return () -> new StringJoiner(delimiter);
      }

      @Override
      public BiConsumer<StringJoiner, Object> accumulator() {
        return (joiner, obj) -> joiner.add(String.valueOf(obj));
      }

      @Override
      public BinaryOperator<StringJoiner> combiner() {
        return StringJoiner::merge;
      }

      @Override
      public Function<StringJoiner, String> finisher() {
        return StringJoiner::toString;
      }

      @Override
      public Set<Characteristics> characteristics() {
        return Collections.emptySet();
      }

      @Override
      public String join(Object l, Object r) {
        return l + delimiter + r;
      }
    };
  }

  /** Joins {@code l} and {@code r} together. */
  public abstract String join(Object l, Object r);

  /** Returns an instance that wraps the join result in the pair of {@code open} and {@code close}. */
  public final Joiner between(char open, char close) {
    return between(Character.toString(open), Character.toString(close));
  }

  /** Returns an instance that wraps the join result in the pair of {@code open} and {@code close}. */
  public final Joiner between(String open, String close) {
    requireNonNull(open);
    requireNonNull(close);
    Joiner base = this;
    return new Joiner() {
      @Override
      public Supplier<StringJoiner> supplier() {
        return base.supplier();
      }

      @Override
      public BiConsumer<StringJoiner, Object> accumulator() {
        return base.accumulator();
      }

      @Override
      public BinaryOperator<StringJoiner> combiner() {
        return base.combiner();
      }

      @Override
      public Function<StringJoiner, String> finisher() {
       return base.finisher().andThen(r -> open + r + close);
      }

      @Override
      public Set<Characteristics> characteristics() {
        return Collections.emptySet();
      }

      @Override
      public String join(Object l, Object r) {
        return open + base.join(l, r) + close;
      }
    };
  }

  Joiner() {}
}
