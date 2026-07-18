package com.company.rpw.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 预警规则表达式引擎。
 *
 * <p>支持语法（大小写不敏感，并兼容中文关键字/全角符号）：
 * <ul>
 *   <li>标识符：对象属性（中文名）或系统属性（当前日期/当前时间/今天）</li>
 *   <li>常量：数字、字符串('或")、true/false/null</li>
 *   <li>函数：isnull(x)、today()、now()、ifelse(条件,真值,假值)</li>
 *   <li>运算：+ - * / ，以及日期相减得到天数（date - date = 天）</li>
 *   <li>比较：&lt; &gt; &lt;= &gt;= == = != （支持链式比较 如 3 &lt; a &lt; 7）</li>
 *   <li>逻辑：and/且、or/或、not/非</li>
 *   <li>括号分组</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class WarningExpressionEngine {

    // ===================== 词法 =====================

    private enum TT { NUMBER, STRING, IDENT, OP, LPAREN, RPAREN, COMMA, AND, OR, NOT, TRUE, FALSE, NULL }

    private static class Token {
        final TT type;
        final String text;
        final Object value;
        Token(TT type, String text) { this(type, text, null); }
        Token(TT type, String text, Object value) { this.type = type; this.text = text; this.value = value; }
    }

    private static final Map<String, String> FULLWIDTH = new HashMap<>();
    static {
        FULLWIDTH.put("（", "("); FULLWIDTH.put("）", ")");
        FULLWIDTH.put("，", ","); FULLWIDTH.put("；", ";");
        FULLWIDTH.put("＜", "<"); FULLWIDTH.put("＞", ">");
        FULLWIDTH.put("＜＝", "<="); FULLWIDTH.put("＞＝", ">=");
        FULLWIDTH.put("＝", "="); FULLWIDTH.put("！＝", "!=");
        FULLWIDTH.put("＋", "+"); FULLWIDTH.put("－", "-");
        FULLWIDTH.put("×", "*"); FULLWIDTH.put("÷", "/");
        FULLWIDTH.put("　", " ");
        // 全角数字
        FULLWIDTH.put("０", "0"); FULLWIDTH.put("１", "1"); FULLWIDTH.put("２", "2");
        FULLWIDTH.put("３", "3"); FULLWIDTH.put("４", "4"); FULLWIDTH.put("５", "5");
        FULLWIDTH.put("６", "6"); FULLWIDTH.put("７", "7"); FULLWIDTH.put("８", "8");
        FULLWIDTH.put("９", "9");
    }

    private static String normalize(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            // 优先匹配双字符全角（≤ ≥ ≠）
            if (i + 1 < input.length()) {
                String two = input.substring(i, i + 2);
                if (FULLWIDTH.containsKey(two)) {
                    sb.append(FULLWIDTH.get(two));
                    i += 2;
                    continue;
                }
            }
            char c = input.charAt(i);
            String one = String.valueOf(c);
            if (FULLWIDTH.containsKey(one)) {
                sb.append(FULLWIDTH.get(one));
            } else {
                sb.append(c);
            }
            i++;
        }
        return sb.toString();
    }

    private static boolean isChinese(char c) {
        return c >= 0x4E00 && c <= 0x9FA5;
    }

    private List<Token> tokenize(String input) {
        String s = normalize(input);
        List<Token> tokens = new ArrayList<>();
        int i = 0, n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            // 数字
            if ((c >= '0' && c <= '9') || (c == '.' && i + 1 < n && Character.isDigit(s.charAt(i + 1)))) {
                int j = i;
                StringBuilder num = new StringBuilder();
                while (j < n && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.')) {
                    num.append(s.charAt(j++));
                }
                String txt = num.toString();
                if (txt.contains(".")) {
                    tokens.add(new Token(TT.NUMBER, txt, Double.parseDouble(txt)));
                } else {
                    tokens.add(new Token(TT.NUMBER, txt, Long.parseLong(txt)));
                }
                i = j;
                continue;
            }
            // 字符串
            if (c == '\'' || c == '"') {
                char q = c;
                int j = i + 1;
                StringBuilder str = new StringBuilder();
                while (j < n && s.charAt(j) != q) {
                    str.append(s.charAt(j++));
                }
                tokens.add(new Token(TT.STRING, str.toString(), str.toString()));
                i = j + 1;
                continue;
            }
            if (c == '(') { tokens.add(new Token(TT.LPAREN, "(")); i++; continue; }
            if (c == ')') { tokens.add(new Token(TT.RPAREN, ")")); i++; continue; }
            if (c == ',') { tokens.add(new Token(TT.COMMA, ",")); i++; continue; }
            // 运算符
            if ("+-*/<>!=".indexOf(c) >= 0) {
                char next = i + 1 < n ? s.charAt(i + 1) : '\0';
                String op;
                if (c == '<' && next == '=') { op = "<="; i += 2; }
                else if (c == '>' && next == '=') { op = ">="; i += 2; }
                else if (c == '=' && next == '=') { op = "=="; i += 2; }
                else if (c == '!' && next == '=') { op = "!="; i += 2; }
                else { op = String.valueOf(c); i += 1; }
                tokens.add(new Token(TT.OP, op));
                continue;
            }
            // 标识符 / 关键字
            if (Character.isLetter(c) || c == '_' || isChinese(c)) {
                int j = i;
                StringBuilder word = new StringBuilder();
                while (j < n && (Character.isLetterOrDigit(s.charAt(j)) || s.charAt(j) == '_' || isChinese(s.charAt(j)))) {
                    word.append(s.charAt(j++));
                }
                String w = word.toString();
                TT kw = keyword(w);
                if (kw != null) {
                    tokens.add(new Token(kw, w));
                } else {
                    tokens.add(new Token(TT.IDENT, w, w));
                }
                i = j;
                continue;
            }
            throw new RuntimeException("表达式包含无法识别的字符: '" + c + "'");
        }
        return tokens;
    }

    private static TT keyword(String w) {
        String lower = w.toLowerCase();
        return switch (lower) {
            case "and", "且" -> TT.AND;
            case "or", "或" -> TT.OR;
            case "not", "非" -> TT.NOT;
            case "true" -> TT.TRUE;
            case "false" -> TT.FALSE;
            case "null" -> TT.NULL;
            default -> null;
        };
    }

    // ===================== 语法（递归下降解释器） =====================

    private List<Token> tokens;
    private int pos;
    private Map<String, Object> variables;

    /** 评估表达式，返回布尔结果 */
    public boolean evaluate(String expr, Map<String, Object> variables) {
        if (expr == null || expr.trim().isEmpty()) {
            return false;
        }
        this.variables = variables == null ? new HashMap<>() : variables;
        this.tokens = tokenize(expr);
        this.pos = 0;
        Object result = parseOr();
        return toBool(result);
    }

    /** 校验表达式语法（使用对象类型的虚拟变量），返回错误信息；合法返回 null */
    public String validate(String expr, String objectType,
                           WarningAttributeRegistry registry) {
        Map<String, Object> dummy = new HashMap<>();
        if (registry != null) {
            for (WarningAttributeRegistry.AttributeMeta a : registry.getAttributes(objectType)) {
                dummy.put(a.label, switch (a.type) {
                    case DATE -> LocalDate.now();
                    case NUMBER -> 0L;
                    case BOOLEAN -> false;
                    default -> "";
                });
            }
        }
        dummy.put("当前日期", LocalDate.now());
        dummy.put("当前时间", LocalDate.now());
        dummy.put("今天", LocalDate.now());
        try {
            evaluate(expr, dummy);
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /** 将表达式中的变量标识符替换为实际值（函数名后接 ( 视为函数调用，原样保留），用于生成预警原因 */
    public String renderWithValues(String expr, Map<String, Object> variables) {
        if (expr == null) return "";
        List<Token> toks = tokenize(expr);
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < toks.size(); k++) {
            Token t = toks.get(k);
            if (t.type == TT.IDENT) {
                boolean isFunction = k + 1 < toks.size() && toks.get(k + 1).type == TT.LPAREN;
                if (isFunction) {
                    sb.append(t.text);
                } else {
                    sb.append(valueString(variables == null ? null : variables.get(t.text)));
                }
            } else {
                sb.append(t.text);
            }
        }
        return sb.toString();
    }

    private Token peek() {
        return pos < tokens.size() ? tokens.get(pos) : new Token(null, "<EOF>");
    }

    private Token consume() {
        return tokens.get(pos++);
    }

    private boolean check(TT type) {
        Token t = peek();
        return t.type == type;
    }

    private Object parseOr() {
        Object left = parseAnd();
        while (peek().type == TT.OR) {
            consume();
            Object right = parseAnd();
            left = toBool(left) || toBool(right);
        }
        return left;
    }

    private Object parseAnd() {
        Object left = parseNot();
        while (peek().type == TT.AND) {
            consume();
            Object right = parseNot();
            left = toBool(left) && toBool(right);
        }
        return left;
    }

    private Object parseNot() {
        if (peek().type == TT.NOT) {
            consume();
            return !toBool(parseNot());
        }
        return parseComparison();
    }

    private Object parseComparison() {
        Object left = parseAdd();
        boolean hasComp = false;
        boolean result = true;
        while (peek().type == TT.OP && isCompOp(peek().text)) {
            String op = consume().text;
            Object right = parseAdd();
            boolean cmp = evalCompare(left, op, right);
            result = result && cmp;
            hasComp = true;
            left = right; // 链式比较：a < b < c 的下一轮以 b 为左操作数
        }
        return hasComp ? result : left;
    }

    private static boolean isCompOp(String op) {
        return switch (op) {
            case "<", ">", "<=", ">=", "==", "=", "!=" -> true;
            default -> false;
        };
    }

    private Object parseAdd() {
        Object left = parseMul();
        while (peek().type == TT.OP && (peek().text.equals("+") || peek().text.equals("-"))) {
            String op = consume().text;
            Object right = parseMul();
            left = arith(left, op, right);
        }
        return left;
    }

    private Object parseMul() {
        Object left = parseUnary();
        while (peek().type == TT.OP && (peek().text.equals("*") || peek().text.equals("/"))) {
            String op = consume().text;
            Object right = parseUnary();
            left = arith(left, op, right);
        }
        return left;
    }

    private Object parseUnary() {
        if (peek().type == TT.OP && peek().text.equals("-")) {
            consume();
            return negate(parseUnary());
        }
        if (peek().type == TT.OP && peek().text.equals("+")) {
            consume();
            return parseUnary();
        }
        return parsePrimary();
    }

    private Object parsePrimary() {
        Token t = peek();
        if (t.type == TT.NUMBER) { consume(); return t.value; }
        if (t.type == TT.STRING) { consume(); return t.value; }
        if (t.type == TT.TRUE) { consume(); return Boolean.TRUE; }
        if (t.type == TT.FALSE) { consume(); return Boolean.FALSE; }
        if (t.type == TT.NULL) { consume(); return null; }
        if (t.type == TT.LPAREN) {
            consume();
            Object v = parseOr();
            if (peek().type != TT.RPAREN) throw new RuntimeException("缺少右括号 )");
            consume();
            return v;
        }
        if (t.type == TT.IDENT) {
            consume();
            String name = t.text;
            if (peek().type == TT.LPAREN) {
                consume(); // (
                List<Object> args = new ArrayList<>();
                if (peek().type != TT.RPAREN) {
                    args.add(parseOr());
                    while (peek().type == TT.COMMA) {
                        consume();
                        args.add(parseOr());
                    }
                }
                if (peek().type != TT.RPAREN) throw new RuntimeException("函数调用缺少右括号 )");
                consume();
                return callFunc(name, args);
            }
            return variables.get(name);
        }
        throw new RuntimeException("表达式语法错误，位置附近: " + t.text);
    }

    private Object callFunc(String name, List<Object> args) {
        String lower = name.toLowerCase();
        return switch (lower) {
            case "isnull" -> {
                if (args.size() != 1) throw new RuntimeException("isnull() 需要 1 个参数");
                yield args.get(0) == null;
            }
            case "today", "now" -> {
                if (!args.isEmpty()) throw new RuntimeException(name + "() 不需要参数");
                yield LocalDate.now();
            }
            case "ifelse" -> {
                if (args.size() != 3) throw new RuntimeException("ifelse() 需要 3 个参数: ifelse(条件, 真值, 假值)");
                yield toBool(args.get(0)) ? args.get(1) : args.get(2);
            }
            default -> throw new RuntimeException("不支持的函数: " + name);
        };
    }

    // ===================== 语义 =====================

    private boolean evalCompare(Object left, String op, Object right) {
        if (op.equals("==") || op.equals("=") || op.equals("!=")) {
            boolean eq = equalsValue(left, right);
            return op.equals("!=") ? !eq : eq;
        }
        // 关系运算
        int cmp;
        if (left == null || right == null) {
            return false;
        }
        try {
            cmp = compareValue(left, right);
        } catch (RuntimeException e) {
            throw new RuntimeException("比较运算不支持的数据类型: " + op, e);
        }
        return switch (op) {
            case "<" -> cmp < 0;
            case ">" -> cmp > 0;
            case "<=" -> cmp <= 0;
            case ">=" -> cmp >= 0;
            default -> throw new RuntimeException("未知比较运算符: " + op);
        };
    }

    private boolean equalsValue(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (isTemporal(a) && isTemporal(b)) {
            return toLocalDate(a).equals(toLocalDate(b));
        }
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        return a.toString().equals(b.toString());
    }

    private int compareValue(Object a, Object b) {
        if (isTemporal(a) && isTemporal(b)) {
            return toLocalDate(a).compareTo(toLocalDate(b));
        }
        if (a instanceof Number && b instanceof Number) {
            return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
        }
        if (a instanceof String && b instanceof String) {
            return ((String) a).compareTo((String) b);
        }
        throw new RuntimeException("不支持的比较: " + a + " 与 " + b);
    }

    private Object arith(Object left, String op, Object right) {
        // 日期运算
        if (isTemporal(left) && isTemporal(right)) {
            if (!op.equals("-")) throw new RuntimeException("两个日期仅支持相减（得到天数）");
            // 返回 left - right 的天数（left 较晚时为正）
            return ChronoUnit.DAYS.between(toLocalDate(right), toLocalDate(left));
        }
        if (isTemporal(left) && right instanceof Number) {
            long days = ((Number) right).longValue();
            return op.equals("-") ? plusDays(left, -days) : plusDays(left, days);
        }
        if (left instanceof Number && isTemporal(right)) {
            if (op.equals("+")) {
                long days = ((Number) left).longValue();
                return plusDays(right, days);
            }
            throw new RuntimeException("数字减日期无意义");
        }
        if (left instanceof Number && right instanceof Number) {
            double x = ((Number) left).doubleValue();
            double y = ((Number) right).doubleValue();
            return switch (op) {
                case "+" -> x + y;
                case "-" -> x - y;
                case "*" -> x * y;
                case "/" -> y == 0 ? 0d : x / y;
                default -> throw new RuntimeException("未知运算符: " + op);
            };
        }
        throw new RuntimeException("不支持的运算: " + left + " " + op + " " + right);
    }

    private Object negate(Object v) {
        if (v instanceof Number) {
            if (v instanceof Double) return -((Double) v);
            if (v instanceof Long) return -((Long) v);
            return -((Number) v).doubleValue();
        }
        throw new RuntimeException("不能对非空数字取负: " + v);
    }

    private boolean toBool(Object o) {
        if (o instanceof Boolean b) return b;
        if (o == null) return false;
        if (o instanceof Number) return ((Number) o).doubleValue() != 0;
        throw new RuntimeException("逻辑运算需要布尔值，实际为: " + o);
    }

    private static boolean isTemporal(Object o) {
        return o instanceof LocalDate || o instanceof LocalDateTime;
    }

    private static LocalDate toLocalDate(Object o) {
        if (o instanceof LocalDate ld) return ld;
        if (o instanceof LocalDateTime ldt) return ldt.toLocalDate();
        throw new RuntimeException("非日期类型: " + o);
    }

    private static Object plusDays(Object o, long days) {
        if (o instanceof LocalDate ld) return ld.plusDays(days);
        if (o instanceof LocalDateTime ldt) return ldt.plusDays(days);
        throw new RuntimeException("非日期类型: " + o);
    }

    private static String valueString(Object o) {
        if (o == null) return "null";
        if (o instanceof LocalDate || o instanceof LocalDateTime) return o.toString();
        if (o instanceof Number) return o.toString();
        if (o instanceof Boolean) return (Boolean) o ? "true" : "false";
        return "'" + o + "'";
    }
}
