package wyvern.tools.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import wyvern.target.corewyvernIL.effects.Effect;
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
        Set<Effect> expectedEffectBound = makeEffectSet("log");
        TestUtil.doApproxScript(PATH, "approx.userModule1", expectedEffectBound);
    }

    @Test
    public void capabilityPassedIntoMethodPureModule() throws ParseException {
        Set<Effect> expectedEffectBound = makeEffectSet("log");
        TestUtil.doApproxScript(PATH, "approx.userModule2", expectedEffectBound);
    }

    @Test
    public void capabilityExposed() throws ParseException {
        Set<Effect> expectedEffectBound = makeEffectSet("write", "log");
        TestUtil.doApproxScript(PATH, "approx.userModule3", expectedEffectBound);
    }

    @Test
    public void importNewEffect() throws ParseException {
        Set<Effect> expectedEffectBound = makeEffectSet("write");
        TestUtil.doApproxScript(PATH, "approx.userModule4", expectedEffectBound);
    }

    @Test
    public void effectVarTypeMembers() throws ParseException {
        Set<Effect> expectedEffectBound = makeEffectSet("write");
        TestUtil.doApproxScript(PATH, "approx.userModule5", expectedEffectBound);
    }

    @Test
    public void globallyAvailableEffectPureModule() throws ParseException {
        Set<Effect> expectedEffectBound = makeEffectSet();
        TestUtil.doApproxScript(PATH, "approx.userModule6", expectedEffectBound);
    }

    @Test
    public void globallyAvailableEffectResourceModule() throws ParseException {
        Set<Effect> expectedEffectBound = makeEffectSet("write");
        TestUtil.doApproxScript(PATH, "approx.userModule7", expectedEffectBound);
    }

    @Test
    public void nonEmptyGloballyAvailableEffectPureModule() throws ParseException {
        Set<Effect> expectedEffectBound = makeEffectSet();
        TestUtil.doApproxScript(PATH, "approx.userModule8", expectedEffectBound);
    }
}
