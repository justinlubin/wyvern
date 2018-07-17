package wyvern.tools.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import wyvern.target.corewyvernIL.expression.StringLiteral;
import wyvern.target.corewyvernIL.support.Util;
import wyvern.tools.imports.extensions.WyvernResolver;
import wyvern.tools.parsing.coreparser.ParseException;
import wyvern.tools.tests.suites.CurrentlyBroken;
import wyvern.tools.tests.suites.RegressionTests;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Category(RegressionTests.class)
public class EffectApproximationTests {
    private static final String PATH = TestUtil.BASE_PATH;

    private static Set<String> effectSet(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    private static void test(int n, String... names) throws ParseException {
        TestUtil.doTestScriptModularly(
                PATH,
                "effectApproximation.testUserModule" + n,
                Util.stringType(),
                new StringLiteral("a")
        );
        TestUtil.doEffectApproximation(
                PATH,
                "effectApproximation.userModule" + n,
                effectSet(names)
        );
    }

    @BeforeClass
    public static void setupResolver() {
        TestUtil.setPaths();
        WyvernResolver.getInstance().addPath(PATH);
    }

    @Test
    public void capabilityPassedIntoFunctorResourceModule() throws ParseException {
        test(1, "Logger.log");
    }

    @Test
    public void capabilityPassedIntoMethodPureModule() throws ParseException {
        test(2, "Logger.log");
    }

    @Test
    public void capabilityExposed() throws ParseException {
        test(3, "File.write", "LoggerExposed.log");
    }

    @Test
    public void importNewEffect() throws ParseException {
        test(4, "File.write", "Logger.log");
    }

    @Test
    public void effectVarTypeMembers() throws ParseException {
        test(5, "File.write");
    }

    @Test
    public void globallyAvailableEffectPureModule() throws ParseException {
        test(6, "effectApproximation.lib6.myEffect");
    }

    @Test
    public void globallyAvailableEffectResourceModule() throws ParseException {
        test(7, "File.write", "effectApproximation.lib6.myEffect");
    }

    @Test
    public void nonEmptyGloballyAvailableEffectPureModule() throws ParseException {
        test(8, "effectApproximation.lib8.myEffect");
    }

    @Test
    @Category(CurrentlyBroken.class)
    public void effectDefinedInValWithMethod() throws ParseException {
        test(9, "");
    }

    @Test
    public void effectDefinedInVal() throws ParseException {
        test(10, "effectApproximation.userModule10.myObject.myEffect");
    }
}
