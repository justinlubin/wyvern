package wyvern.tools.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import wyvern.target.corewyvernIL.effects.Effect;
import wyvern.target.corewyvernIL.expression.StringLiteral;
import wyvern.target.corewyvernIL.support.Util;
import wyvern.tools.imports.extensions.WyvernResolver;
import wyvern.tools.parsing.coreparser.ParseException;
import wyvern.tools.tests.suites.RegressionTests;

import java.util.HashSet;
import java.util.Set;

@Category(RegressionTests.class)
public class EffectApproximationTests {
    private static final String PATH = TestUtil.BASE_PATH;

    private static Set<Effect> makeEffectSet(String... names) {
        Set<Effect> effectSet = new HashSet<>();
        for (String name : names) {
            effectSet.add(new Effect(null, name, null));
        }
        return effectSet;
    }

    @BeforeClass
    public static void setupResolver() {
        TestUtil.setPaths();
        WyvernResolver.getInstance().addPath(PATH);
    }

    @Test
    public void capabilityPassedIntoFunctorResourceModule() throws ParseException {
        TestUtil.doTestScriptModularly(
                PATH,
                "effectApproximation.testUserModule1",
                Util.stringType(),
                new StringLiteral("a")
        );
        Set<Effect> expectedEffectBound = makeEffectSet("log");
        TestUtil.doApprox(PATH, "effectApproximation.userModule1", expectedEffectBound);
    }

    @Test
    public void capabilityPassedIntoMethodPureModule() throws ParseException {
        TestUtil.doTestScriptModularly(
                PATH,
                "effectApproximation.testUserModule2",
                Util.stringType(),
                new StringLiteral("a")
        );
        Set<Effect> expectedEffectBound = makeEffectSet("log");
        TestUtil.doApprox(PATH, "effectApproximation.userModule2", expectedEffectBound);
    }

    @Test
    public void capabilityExposed() throws ParseException {
        TestUtil.doTestScriptModularly(
                PATH,
                "effectApproximation.testUserModule3",
                Util.stringType(),
                new StringLiteral("a")
        );
        Set<Effect> expectedEffectBound = makeEffectSet("write", "log");
        TestUtil.doApprox(PATH, "effectApproximation.userModule3", expectedEffectBound);
    }

    @Test
    public void importNewEffect() throws ParseException {
        TestUtil.doTestScriptModularly(
                PATH,
                "effectApproximation.testUserModule4",
                Util.stringType(),
                new StringLiteral("a")
        );
        Set<Effect> expectedEffectBound = makeEffectSet("write", "log");
        TestUtil.doApprox(PATH, "effectApproximation.userModule4", expectedEffectBound);
    }

    @Test
    public void effectVarTypeMembers() throws ParseException {
        TestUtil.doTestScriptModularly(
                PATH,
                "effectApproximation.testUserModule5",
                Util.stringType(),
                new StringLiteral("a")
        );
        Set<Effect> expectedEffectBound = makeEffectSet("write");
        TestUtil.doApprox(PATH, "effectApproximation.userModule5", expectedEffectBound);
    }

    @Test
    public void globallyAvailableEffectPureModule() throws ParseException {
        TestUtil.doTestScriptModularly(
                PATH,
                "effectApproximation.testUserModule6",
                Util.stringType(),
                new StringLiteral("a")
        );
        Set<Effect> expectedEffectBound = makeEffectSet("myEffect");
        TestUtil.doApprox(PATH, "effectApproximation.userModule6", expectedEffectBound);
    }

    @Test
    public void globallyAvailableEffectResourceModule() throws ParseException {
        TestUtil.doTestScriptModularly(
                PATH,
                "effectApproximation.testUserModule7",
                Util.stringType(),
                new StringLiteral("a")
        );
        Set<Effect> expectedEffectBound = makeEffectSet("write", "myEffect");
        TestUtil.doApprox(PATH, "effectApproximation.userModule7", expectedEffectBound);
    }

    @Test
    public void nonEmptyGloballyAvailableEffectPureModule() throws ParseException {
        TestUtil.doTestScriptModularly(
                PATH,
                "effectApproximation.testUserModule8",
                Util.stringType(),
                new StringLiteral("a")
        );
        Set<Effect> expectedEffectBound = makeEffectSet("myEffect");
        TestUtil.doApprox(PATH, "effectApproximation.userModule8", expectedEffectBound);
    }
}
