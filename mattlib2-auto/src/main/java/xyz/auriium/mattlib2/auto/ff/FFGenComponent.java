package xyz.auriium.mattlib2.auto.ff;

import xyz.auriium.mattlib2.log.INetworkedComponent;
import xyz.auriium.mattlib2.log.annote.Conf;
import xyz.auriium.mattlib2.log.annote.Log;

public interface FFGenComponent extends INetworkedComponent {



    @Conf("startDelay_ms") long delay_ms();
    @Conf("endVoltage") double endVoltage_volts();
    @Conf("rampRate_vPerMs") double rampRate_voltsPerMS();

    @Log("inputVelocity") void logInputVelocity(double iv);
    @Log("inputVoltage") void logInputVoltage(double iV);

    @Log("predictKS") void logPredictedStaticConstant(double ks);
    @Log("predictKV") void logPredictedVelocityConstant(double kv);


}