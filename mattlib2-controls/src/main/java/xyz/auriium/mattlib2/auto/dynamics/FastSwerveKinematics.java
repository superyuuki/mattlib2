package xyz.auriium.mattlib2.auto.dynamics;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import org.ojalgo.matrix.MatrixR064;

import java.util.Arrays;

public class FastSwerveKinematics {

    final MatrixR064 inverseKinematics;
    final MatrixR064 forwardKinematics_pseudo;

    /**
     * We use this to save the last positions, because when x and y approach 0 the angle disappears
     */
    final Rotation2d[] rotationVector = new Rotation2d[4];

    public FastSwerveKinematics(MatrixR064 inverseKinematics, MatrixR064 forwardKinematics_pseudo) {
        this.inverseKinematics = inverseKinematics;
        this.forwardKinematics_pseudo = forwardKinematics_pseudo;
        Arrays.fill(rotationVector, new Rotation2d());
    }

    public static FastSwerveKinematics load(Translation2d[] swerveModulePositionOffsets_four) {
        //Generate 1st order inverse kinematic matrix

        var inverseKinematicBuilder = MatrixR064.FACTORY.makeDense(8,3);

        for (int row = 0; row < 4; row++) {
            //convert chassis x,y,theta to module [row]'s x
            inverseKinematicBuilder.set(row * 2, 0, 1);
            inverseKinematicBuilder.set(row * 2, 1, 0);
            inverseKinematicBuilder.set(row * 2, 2, -swerveModulePositionOffsets_four[row].getY());

            //convert chassis x,y,theta to module [row]'s y
            inverseKinematicBuilder.set(row * 2 + 1, 0, 0);
            inverseKinematicBuilder.set(row * 2 + 1, 1, 1);
            inverseKinematicBuilder.set(row * 2 + 1, 2, swerveModulePositionOffsets_four[row].getX());
        }

        MatrixR064 inverseKinematics = inverseKinematicBuilder.get();
        MatrixR064 forwardKinematics_pseudo = (inverseKinematics.transpose().multiply(inverseKinematics)).invert().multiply(inverseKinematics.transpose());

                //FUCKING GIVE ME THE MOORE PERSIJISNINFOSNFOSNFAISNFO
        //DUMB SHIT

        return new FastSwerveKinematics(inverseKinematics, forwardKinematics_pseudo);
    }


    public SwerveModuleState[] convertCentroidStateToModuleStateSafe(ChassisSpeeds speeds) {
        if (speeds.vxMetersPerSecond == 0.0
                && speeds.vyMetersPerSecond == 0.0
                && speeds.omegaRadiansPerSecond == 0.0) {
            SwerveModuleState[] moduleStates = new SwerveModuleState[4];
            for (int i = 0; i < 4; i++) {
                moduleStates[i] = new SwerveModuleState(0.0, rotationVector[i]);
            }

            return moduleStates;
        }

        MatrixR064 centroidStateVector = MatrixR064.FACTORY.column(speeds.vxMetersPerSecond, speeds.vyMetersPerSecond, speeds.omegaRadiansPerSecond);
        MatrixR064 moduleStateVector = convertCentroidStateToModuleState(centroidStateVector); //compiler inlines this

        SwerveModuleState[] locallyAllocatedStates = new SwerveModuleState[4]; //TODO reduce object allocations

        for (int i = 0; i < 4; i++) {
            double x = moduleStateVector.doubleValue(i * 2);
            double y = moduleStateVector.doubleValue(i * 2  + 1);

            double velocity = Math.hypot(x,y);
            Rotation2d angle = new Rotation2d(x,y);

            locallyAllocatedStates[i] = new SwerveModuleState(velocity, angle);
            rotationVector[i] = angle;
        }

        return locallyAllocatedStates;

    }

    public ChassisSpeeds convertModuleStateToCentroidState(SwerveModuleState[] states) {

        MatrixR064 moduleStateVector = MatrixR064.FACTORY.column(
                states[0].speedMetersPerSecond * states[0].angle.getSin(),
                states[0].speedMetersPerSecond * states[0].angle.getCos(),
                states[1].speedMetersPerSecond * states[1].angle.getSin(),
                states[1].speedMetersPerSecond * states[1].angle.getCos(),
                states[2].speedMetersPerSecond * states[2].angle.getSin(),
                states[2].speedMetersPerSecond * states[2].angle.getCos(),
                states[3].speedMetersPerSecond * states[3].angle.getSin(),
                states[3].speedMetersPerSecond * states[3].angle.getCos()
        );


        MatrixR064 centroidStateVector = convertModuleStateToCentroidState(moduleStateVector);

        return new ChassisSpeeds(
                centroidStateVector.doubleValue(0),
                centroidStateVector.doubleValue(1),
                centroidStateVector.doubleValue(2)
        );
    }

    /**
     *
     * @param centroidVelocityVector 3x1 matrix of [x,y,theta]
     * @return 8x1 matrix of module states [x,y,x,y,x,y,x,y]
     */

    public MatrixR064 convertCentroidStateToModuleState(MatrixR064 centroidVelocityVector) {
        return inverseKinematics.multiply(centroidVelocityVector);
    }

    /**
     *
     * @param moduleVelocityVector 8x1 matrix of module states [x,y,x,y,x,y,x,y]
     * @return 3x1 matrix of x,y,theta
     */
    public MatrixR064 convertModuleStateToCentroidState(MatrixR064 moduleVelocityVector) {
        return forwardKinematics_pseudo.multiply(moduleVelocityVector);
    }

}
