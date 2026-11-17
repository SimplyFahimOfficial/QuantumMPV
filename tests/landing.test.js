import { describe, expect, it } from "vitest";
import { TestDriver } from "testdriverai/vitest/hooks";

describe("QuantumMPV Landing Page", () => {
  it("should load the landing page and navigate to support section", async (context) => {
    const testdriver = TestDriver(context);

    await testdriver.provision.chrome({ url: "https://quantummpv.vercel.app" });

    const heroAssert = await testdriver.assert(
      "The QuantumMPV heading and Download button are visible on the page"
    );
    expect(heroAssert).toBeTruthy();

    await testdriver.find("Support me button").click();
    await testdriver.find("Support the Project heading", { timeout: 15000 });

    const supportAssert = await testdriver.assert(
      "The Support the Project section is visible with payee Md Fahim and UPI ID"
    );
    expect(supportAssert).toBeTruthy();
  });
});
