/**
 * Copyright 2017 XEBIALABS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ext.deployit.community.plugin.restrictplaceholders.util;

import static ext.deployit.community.plugin.restrictplaceholders.util.Dictionaries.consolidatedDictionary;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import com.xebialabs.deployit.booter.local.LocalBooter;
import com.xebialabs.deployit.plugin.api.udm.Dictionary;
import com.xebialabs.deployit.plugin.api.udm.Environment;

/**
 * Unit tests for {@link Dictionaries}
 */
public class DictionariesTest {

    @Before
    public void setup() {
        LocalBooter.bootWithoutGlobalContext();
    }

    @Test
    public void consolidatedDictionariesFlattensAndResolves() {
        Dictionary base = new Dictionary();
        base.getEntries().putAll(
            ImmutableMap.of("FOO", "foo", "BAR", "{{FOO}} and bar"));
        Dictionary override = new Dictionary();
        override.getEntries().putAll(
            ImmutableMap.of("BAZ", "{{FOO}} and {{BAR}} and baz", "FOO", "overridden foo"));
        Environment environment = new Environment();
        // earlier items are "higher up in the stack" than later ones
        environment.getDictionaries().addAll(ImmutableList.of(override, base));

        assertEquals(ImmutableMap.of("FOO", "overridden foo",
                "BAR", "overridden foo and bar",
                "BAZ", "overridden foo and overridden foo and bar and baz"), 
                consolidatedDictionary(environment));
    }

    @Test
    public void handlesValuesWithDollarCharacters() {
        Dictionary dict = new Dictionary();
        dict.getEntries().putAll(
            ImmutableMap.of("FOO", "value-with-$", "BAR", "{{FOO}} and bar"));
        Environment environment = new Environment();
        // earlier items are "higher up in the stack" than later ones
        environment.getDictionaries().addAll(ImmutableList.of(dict));

        assertEquals(ImmutableMap.of("FOO", "value-with-$",
                "BAR", "value-with-$ and bar"),
                consolidatedDictionary(environment));
    }

    @Test(expected = IllegalStateException.class)
    public void consolidatedDictionariesThrowsExceptionOnSelfReference() {
        // four-link chain: FOO -> BAR -> BOZ -> FOZ -> FOO
        Dictionary one = new Dictionary();
        one.getEntries().putAll(
            ImmutableMap.of("FOO", "{{BAZ}} and {{BAR}}", "BAR", "{{BAZ}} and {{BOZ}}", "BAZ", "baz", "BOZ", "{{BAZ}} and {{FOZ}}"));
        Dictionary two = new Dictionary();
        two.getEntries().putAll(ImmutableMap.of("FOZ", "foz and {{FOO}}"));
        Environment environment = new Environment();
        environment.getDictionaries().addAll(ImmutableList.of(one, two));
        consolidatedDictionary(environment);
    }

}
