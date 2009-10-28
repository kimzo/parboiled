package org.parboiled;

import org.jetbrains.annotations.NotNull;
import org.parboiled.support.Chars;
import org.parboiled.support.Checks;
import org.parboiled.support.ParserConstructionException;
import static org.parboiled.utils.Utils.arrayOf;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public abstract class BaseParser<T extends Actions> {

    public static final Object NULL = new Object();

    private final Map<Character, Rule> charMatchers = new HashMap<Character, Rule>();
    private final Map<String, Rule> stringMatchers = new HashMap<String, Rule>();
    private final LinkedList<ActionParameter> actionParameterStack = new LinkedList<ActionParameter>();
    public final T actions;

    ///************************* PUBLIC ***************************///

    public BaseParser(T actions) {
        this.actions = actions;
    }

    public Rule ch(char c) {
        return new CharMatcher(c);
    }

    public Rule charRange(char cLow, char cHigh) {
        return cLow == cHigh ? ch(cLow) : new CharRangeMatcher(cLow, cHigh);
    }

    public Rule string(@NotNull String string) {
        Rule[] matchers = new Rule[string.length()];
        for (int i = 0; i < string.length(); i++) matchers[i] = cachedChar(string.charAt(i));
        return new SequenceMatcher(matchers, false).label(string);
    }

    public Rule firstOf(@NotNull Object rule, Object rule2, Object... moreRules) {
        return new FirstOfMatcher(toRules(arrayOf(rule, arrayOf(rule2, moreRules)))).label("firstOf");
    }

    public Rule oneOrMore(@NotNull Object rule) {
        return new OneOrMoreMatcher(toRule(rule)).label("oneOrMore");
    }

    public Rule optional(@NotNull Object rule) {
        return new OptionalMatcher(toRule(rule)).label("optional");
    }

    public Rule sequence(@NotNull Object rule, Object... moreRules) {
        return new SequenceMatcher(toRules(arrayOf(rule, moreRules)), false).label("sequence");
    }

    public Rule enforcedSequence(@NotNull Object rule, Object... moreRules) {
        return new SequenceMatcher(toRules(arrayOf(rule, moreRules)), true).label("enforcedSequence");
    }

    public Rule test(@NotNull Object rule) {
        return new TestMatcher(toRule(rule), false);
    }

    public Rule testNot(@NotNull Object rule) {
        return new TestMatcher(toRule(rule), true);
    }

    public Rule zeroOrMore(@NotNull Object rule) {
        return new ZeroOrMoreMatcher(toRule(rule)).label("zeroOrMore");
    }

    public Rule eof() {
        return ch(Chars.EOF);
    }

    public Rule any() {
        return ch(Chars.ANY);
    }

    public Rule empty() {
        return ch(Chars.EMPTY);
    }

    // action parameters

    public Node node(@NotNull String path) {
        actionParameterStack.add(new ActionParameter.Node(path));
        return null;
    }

    public Node[] nodes(@NotNull String path) {
        actionParameterStack.add(new ActionParameter.Nodes(path));
        return null;
    }

    public Object value(String path) {
        return value(path, null);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public <T> T value(String path, Class<T> type) {
        actionParameterStack.add(new ActionParameter.Value(path));
        return null;
    }

    public Object[] values(String path) {
        return values(path, null);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public <T> T[] values(String path, Class<T> type) {
        actionParameterStack.add(new ActionParameter.Values(path));
        return null;
    }

    public String text(String path) {
        actionParameterStack.add(new ActionParameter.Text(path));
        return null;
    }

    public String[] texts(String path) {
        actionParameterStack.add(new ActionParameter.Texts(path));
        return null;
    }

    public Character ch(String path) {
        actionParameterStack.add(new ActionParameter.Char(path));
        return null;
    }

    public Character[] chars(String path) {
        actionParameterStack.add(new ActionParameter.Chars(path));
        return null;
    }

    @SuppressWarnings({"unchecked"})
    public <T> T firstNonNull(T value, T... moreValues) {
        T[] args = arrayOf(value, moreValues);
        for (int i = args.length - 1; i >= 0; i--) {
            T arg = args[i];
            if (arg == null) {
                arg = (T) actionParameterStack.removeLast();
                Checks.ensure(arg != null, "Illegal argument list for firstNonNull(): null values are not allowed!");
            }
            args[i] = arg;
        }
        actionParameterStack.add(new ActionParameter.FirstOfNonNull(args));
        return null;
    }

    public <N extends Number> N convertToNumber(String text, Class<N> type) {
        Object arg = text;
        if (text == null) {
            arg = actionParameterStack.removeLast();
            Checks.ensure(arg != null, "Illegal argument list for convertToNumber(): null values are not allowed!");
        }
        actionParameterStack.add(new ActionParameter.ConvertToNumber<N>(arg, type));
        return null;
    }

    ///************************* PACKAGE ***************************///

    ActionParameter[] retrieveAndClearActionParameters() {
        ActionParameter[] params = actionParameterStack.toArray(new ActionParameter[actionParameterStack.size()]);
        actionParameterStack.clear();
        return params;
    }

    ///************************* PRIVATE ***************************///

    private Rule cachedChar(char c) {
        Rule matcher = charMatchers.get(c);
        if (matcher == null) {
            matcher = ((AbstractMatcher) ch(c)).lock();
            charMatchers.put(c, matcher);
        }
        return matcher;
    }

    private Rule cachedString(String string) {
        Rule matcher = stringMatchers.get(string);
        if (matcher == null) {
            matcher = ((AbstractMatcher) string(string)).lock();
            stringMatchers.put(string, matcher);
        }
        return matcher;
    }

    private Rule[] toRules(@NotNull Object[] objects) {
        Rule[] rules = new Rule[objects.length];
        for (int i = 0; i < objects.length; i++) {
            rules[i] = toRule(objects[i]);
        }
        return rules;
    }

    private Rule toRule(Object obj) {
        if (obj instanceof Rule) {
            return (Rule) obj;
        }
        if (obj instanceof Character) {
            return cachedChar((Character) obj);
        }
        if (obj instanceof String) {
            return cachedString((String) obj);
        }
        throw new ParserConstructionException("\'" + obj + "\' is not a valid Rule");
    }

}