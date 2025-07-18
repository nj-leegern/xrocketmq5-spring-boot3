package com.leegern.xrocketmq5.core.enums;

/**
 * 消息选择器类型： TAG或SQL模式
 */
public enum SelectorType {

    /**
     *  Only support or operation such as "tag1 || tag2 || tag3", <br>
     *  If null or * expression,meaning subscribe all.
     */
    TAG,

    /**
     * <ul>
     * Keywords:
     * <li>{@code AND, OR, NOT, BETWEEN, IN, TRUE, FALSE, IS, NULL}</li>
     * </ul>
     * <p/>
     * <ul>
     * Data type:
     * <li>Boolean, like: TRUE, FALSE</li>
     * <li>String, like: 'abc'</li>
     * <li>Decimal, like: 123</li>
     * <li>Float number, like: 3.1415</li>
     * </ul>
     * <p/>
     * <ul>
     * Grammar:
     * <li>{@code AND, OR}</li>
     * <li>{@code >, >=, <, <=, =}</li>
     * <li>{@code BETWEEN A AND B}, equals to {@code >=A AND <=B}</li>
     * <li>{@code NOT BETWEEN A AND B}, equals to {@code >B OR <A}</li>
     * <li>{@code IN ('a', 'b')}, equals to {@code ='a' OR ='b'}, this operation only support String type.</li>
     * <li>{@code IS NULL}, {@code IS NOT NULL}, check parameter whether is null, or not.</li>
     * <li>{@code =TRUE}, {@code =FALSE}, check parameter whether is true, or false.</li>
     * </ul>
     * <p/>
     * <p>
     * Example:
     * (a > 10 AND a < 100) OR (b IS NOT NULL AND b=TRUE)
     * </p>
     */
    SQL92
}
