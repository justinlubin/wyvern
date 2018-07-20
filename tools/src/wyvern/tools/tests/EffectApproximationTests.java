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
        test(1, "logger.log");
    }

    @Test
    public void capabilityPassedIntoMethodPureModule() throws ParseException {
        test(2, "logger.log");
    }

    @Test
    public void capabilityExposed() throws ParseException {
        // Avoidance problem?
        test(3, "logger.update.write", "logger.log");
    }

    @Test
    @Category(CurrentlyBroken.class)
    public void importNewEffect() throws ParseException {
        // Is this even typeable?
        test(4, "file.write", "?.log");
    }

    @Test
    public void effectVarTypeMembers() throws ParseException {
        test(5, "lib.myFile.write");
    }

    @Test
    public void globallyAvailableEffectPureModule() throws ParseException {
        test(6, "lib6.myEffect");
    }

    @Test
    public void globallyAvailableEffectResourceModule() throws ParseException {
        test(7, "file.write", "lib6.myEffect");
    }

    @Test
    public void nonEmptyGloballyAvailableEffectPureModule() throws ParseException {
        test(8, "lib8.myEffect");
    }

    @Test
    @Category(CurrentlyBroken.class)
    public void effectDefinedInValWithMethod() throws ParseException {
        test(9, "");
    }

    @Test
    public void effectDefinedInVal() throws ParseException {
        test(10, "myObject.myEffect");
    }
}
