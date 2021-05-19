package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.*;

public class MMMovement {
    // Abstract values
    private final double[] force = new double[4];
    private final double[] lastForce = new double[4];
    private final ElapsedTime time = new ElapsedTime();

    private double p = 1;
    double currentX = 1;
    double currentY = 1;
    double targetAngle = 0;
    double targetX = 1;
    double targetY = 1;
    double error = 1;
    final double k = 1;

    // values used in the movement
    private double internal;
    private double external;

    // MOTORS
    private DcMotor FR;
    private DcMotor FL;
    private DcMotor BR;
    private DcMotor BL;
    private DcMotor shooterMotor;
    private DcMotor intake;
    private DcMotor armMotor;

    // SERVOS
    private Servo shooterServo;
    private Servo clawServo;

    // IMU variables
    private BNO055IMU imu;
    private final BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();

    /*METHODS USED AT THE START*/
    // Define the robot's hardware
    public void defHardware(HardwareMap hardwareMap) {

        // Motors used in the movement
        FL = hardwareMap.dcMotor.get("FL");
        FR = hardwareMap.dcMotor.get("FR");
        BL = hardwareMap.dcMotor.get("BL");
        BR = hardwareMap.dcMotor.get("BR");

        armMotor = hardwareMap.dcMotor.get("arm_motor");
        shooterMotor = hardwareMap.dcMotor.get("shooter_motor");
        intake = hardwareMap.dcMotor.get("intake_motor");
        shooterServo = hardwareMap.servo.get("trig_servo");
        clawServo = hardwareMap.servo.get("claw_servo");

        armMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        armMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        shooterMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        BL.setDirection(DcMotor.Direction.REVERSE);
        FL.setDirection(DcMotor.Direction.REVERSE);

        FL.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        FR.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        BL.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        BR.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    }

    // Init the IMU variables
    public void initIMU (HardwareMap hardwareMap){
        imu = hardwareMap.get(BNO055IMU.class, "imu");
        parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        parameters.calibrationDataFile = "BNO055IMUCalibration.json";
        parameters.loggingEnabled = true;
        parameters.loggingTag = "IMU";
        parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();
        imu.initialize(parameters);
    }

    /*METHODS USED IN THE AUTONOMOUS PERIOD*/
    // Turn the robot with given force, side, angle and precision
    public void turn(double force, boolean isRight, double targetAngle, final double smoother) {
        double angle;
        final int threshold = 7;
        double currentAngle = getCurrentDegree();
        if (isRight) {
            angle = -targetAngle + currentAngle;
            angle -= threshold;
            if (angle < -180) {
                angle += 360;
            }
            if (angle - currentAngle > 180) {
                currentAngle += 360;
            }
            while (angle + threshold <= currentAngle) {
                currentAngle = getCurrentDegree();
                if (angle - currentAngle > 180)
                    currentAngle += 360;
                if (force < smoother(angle, currentAngle, smoother)) {
                    FR.setPower(force);
                    BR.setPower(force);
                    FL.setPower(-force);
                    BL.setPower(-force);
                } else {
                    FR.setPower(smoother(angle, currentAngle, smoother));
                    BR.setPower(smoother(angle, currentAngle, smoother));
                    FL.setPower(-smoother(angle, currentAngle, smoother));
                    BL.setPower(-smoother(angle, currentAngle, smoother));
                }
            }
        } else {
            angle = targetAngle + currentAngle;
            angle += threshold;
            if (angle > 180) {
                angle -= 360;
            }
            if (currentAngle - angle > 180)
                currentAngle -= 360;
            while (angle - threshold >= currentAngle) {
                currentAngle = getCurrentDegree();
                if (currentAngle - angle > 180)
                    currentAngle -= 360;
                if (force < ((currentAngle - angle) / smoother)) {
                    FR.setPower(-force);
                    BR.setPower(-force);
                    FL.setPower(force);
                    BL.setPower(force);
                } else {
                    FR.setPower(-(currentAngle - angle) / smoother);
                    BR.setPower(-(currentAngle - angle) / smoother);
                    FL.setPower((currentAngle - angle) / smoother);
                    BL.setPower((currentAngle - angle) / smoother);
                }
            }
        }
    }

    /*METHODS USED IN THE TELE-OPERATED PERIOD*/
    // Move the robot
    public void move(double leftY, double leftX, boolean slower, boolean faster) {

        // Create a variable using the IMU
        double angle = getCurrentDegree();

        // Defines the smoother for the movement
        final double smoother = 0.1;

        // Will get the IMU value and change the "internal" and "external" variables
        if (0 < angle && angle <= 90) {
            internal = -(angle / 45 - 1);
            external = 1;
        }
        else if (90   < angle && angle <= 180) {
            internal = -1;
            external = -((angle - 90) / 45 - 1);
        }
        else if (-90  < angle && angle <= 0) {
            internal = 1;
            external = angle / 45 + 1;
        }
        else if (-180 < angle && angle <= 90) {
            internal = (angle + 90) / 45 + 1;
            external = -1;
        }

        // Add to the vector the forces of the gamepad and the angle variables
        force[0] = leftY * internal + leftX * external + p;
        force[1] = leftY * external - leftX * internal + p;
        force[2] = leftY * external - leftX * internal - p;
        force[3] = leftY * internal + leftX * external - p;

        // See if the difference of the last force and the current one is bigger than the
        // smoother value, if it is, it will change gradually to not damage the motors
        if (Math.abs(lastForce[0] - force[0]) > smoother) {
            if (lastForce[0] > force[0]) force[0] = lastForce[0] - smoother;
            else force[0] = lastForce[0] + smoother;
        }
        if (Math.abs(lastForce[1] - force[1]) > smoother) {
            if (lastForce[1] > force[1]) force[1] = lastForce[1] - smoother;
            else force[1] = lastForce[1] + smoother;
        }
        if (Math.abs(lastForce[2] - force[2]) > smoother) {
            if (lastForce[2] > force[2]) force[2] = lastForce[2] - smoother;
            else force[2] = lastForce[2] + smoother;
        }
        if (Math.abs(lastForce[3] - force[3]) > smoother) {
            if (lastForce[3] > force[3]) force[3] = lastForce[3] - smoother;
            else force[3] = lastForce[3] + smoother;
        }

        // Save the used force in variables to get the difference
        lastForce[0] = force[0];
        lastForce[1] = force[1];
        lastForce[2] = force[2];
        lastForce[3] = force[3];

        // If the right bumper is pressed, the robot will move slower
        if (slower) {
            FR.setPower(force[0] / 4);
            BR.setPower(force[1] / 4);
            FL.setPower(force[2] / 4);
            BL.setPower(force[3] / 4);
        }
        // If the right bumper is pressed, the robot will move faster
        else if (faster) {
            FR.setPower(force[0]);
            BR.setPower(force[1]);
            FL.setPower(force[2]);
            BL.setPower(force[3]);
        }
        // If neither bumper is pressed, the robot will move half the speed
        else {
            FR.setPower(force[0] / 2);
            BR.setPower(force[1] / 2);
            FL.setPower(force[2] / 2);
            BL.setPower(force[3] / 2);
        }
    }

    // Define PD variables for the teleOp movement

    public void setPd(double rightX, boolean isActive) {

//        double currentAngle = getCurrentDegree();
//
//        if (rightX > 0.01) {
//            if (targetAngle > -179.9) {
//                targetAngle -= rightX;
//            } else {
//                targetAngle += 359;
//            }
//        }
//        else if (rightX < -0.01) {
//            if (targetAngle < 179.9) {
//                targetAngle -= rightX;
//            } else {
//                targetAngle -= 359;
//            }
//        }
//
//
//        if (0 < currentAngle && currentAngle <= 90) {
//            currentX = -(currentAngle / 45 - 1);
//            currentY = 1;
//        }
//        else if (90 < currentAngle && currentAngle <= 180) {
//            currentX = -1;
//            currentY = -((currentAngle - 90) / 45 - 1);
//        }
//        else if (-90 < currentAngle && currentAngle <= 0) {
//            currentX = 1;
//            currentY = currentAngle / 45 + 1;
//        }
//        else if (-180 < currentAngle && currentAngle <= 90) {
//            currentX = (currentAngle + 90) / 45 + 1;
//            currentY = -1;
//        }
//
//        if (0 < targetAngle && targetAngle <= 90) {
//            targetX = -(targetAngle / 45 - 1);
//            targetY = 1;
//        }
//        else if (90 < targetAngle && targetAngle <= 180) {
//            targetX = -1;
//            targetY = -((targetAngle - 90) / 45 - 1);
//        }
//        else if (-90 < targetAngle && targetAngle <= 0) {
//            targetX = 1;
//            targetY = targetAngle / 45 + 1;
//        }
//        else if (-180 < targetAngle && targetAngle <= 90) {
//            targetX = (targetAngle + 90) / 45 + 1;
//            targetY = -1;
//        }
//
//        if (targetX == 1) {
//            if (currentX == 1) {
//                error = -(targetY - currentY);
//            } else if (currentY == 1) {
//                error = 2;
//            } else if (currentY == -1){
//                error = -2;
//            }
//        }
//        else if (targetX == -1){
//            if (currentX == -1){
//                error = targetY - currentY;
//            } else if (currentY == 1) {
//                error = -2;
//            } else if (currentY == -1){
//                error = 2;
//            }
//        }
//        else if (targetY == 1){
//            if (currentY == 1){
//                error = targetX - currentX;
//            } else if (currentX == 1) {
//                error = -2;
//            } else if (currentX == -1){
//                error = 2;
//            }
//        }
//        else if (targetY == -1){
//            if (currentY == -1){
//                error = - ( targetX - currentX);
//            } else if (currentX == 1) {
//                error = 2;
//            } else if (currentX == -1){
//                error = -2;
//            }
//        }
//
//        final double k = 4;

        if (isActive)
            p = error * k;
        else
            p = rightX;
    }

    // Define the intake force
    public void intakeForce(double force) { intake.setPower(force); }

    // Shoot the rings to the high goals
    public void shoot(boolean trigger)  {

        // Start the shooter motor
        shooterMotor.setPower(0.7);

        // Pull the trigger if a defined button is pressed
        if (trigger) {
            shooterServo.setPosition(0.2);
        } else {
            shooterServo.setPosition(1);
        }
    }

    // Shoot the rings in the power shot
    public void powerShot(boolean trigger)  {
        shooterMotor.setPower(-0.8);
        if (trigger) {
            shooterServo.setPosition(0);
        } else {
            shooterServo.setPosition(0.3);
        }

    }

    // Move the robot's claw
    public void claw(boolean halfDown, boolean totalUp, boolean totalDown, boolean open, boolean close) {

        final int up = 700;
        final int down = -520;
        final int half = 300;
        final double force = 0.7;

        if (open) {
            clawServo.setPosition(1);
        } else if (close) {
            clawServo.setPosition(0);
        }

        if (totalUp){
            armMotor.setTargetPosition(up);
            armMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            armMotor.setPower(-force);
        }
        else if (totalDown) {
            armMotor.setTargetPosition(down);
            armMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            armMotor.setPower(force);
        }
        else if (halfDown) {
            armMotor.setTargetPosition(half);
            armMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            armMotor.setPower(force);
        }

        if (!armMotor.isBusy()){
            armMotor.setPower(0);
        }
    }

    // Use some values to make an variable value that should decrease the robot velocity
    private double smoother(double firstValue, double secondValue, double strength){
        double smoother;
        smoother = (firstValue - secondValue) / strength;
        return smoother;
    }

    /*Getters and setters*/
    public double getFlForce(){ return FL.getPower(); }
    public double getFrForce(){ return FR.getPower(); }
    public double getBlForce(){ return BL.getPower(); }
    public double getBrForce(){ return BR.getPower(); }
    public double getIntakeForce(){ return intake.getPower(); }
    public double getArmEncoder() { return armMotor.getCurrentPosition(); }
    private double getCurrentDegree() {return imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES).firstAngle;}
    private double getCurrentRadians() {return imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.RADIANS).firstAngle;}
}