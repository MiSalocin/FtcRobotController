package org.firstinspires.ftc.teamcode;

//import the FTC libraries to use in the program
import com.qualcomm.robotcore.eventloop.opmode.*;

import static java.lang.Thread.sleep;

@TeleOp(name = "OpMode", group = "Bahtech")
public class MMOpMode extends OpMode {

    double x = 1;
    final MMMovementTO moveT = new MMMovementTO();
    final MMMovementAU moveA = new MMMovementAU();

    @Override
    public void init() {
        telemetry.addData("Aperte START para começar", "");
        moveA.defHardware(hardwareMap);
    }

    // Código que será executado após apertarmos START.
    @Override
    public void loop() {
        // Mostra os botoes em uso na tela.
        telemetry.addData("Stick Esquerdo X", gamepad1.left_stick_x);
        telemetry.addData("Stick Esquerdo Y", gamepad1.left_stick_y);
        telemetry.addData("Stick Direito X", gamepad1.right_stick_x);
        telemetry.addData("Força", x);

        if (gamepad1.x)
            x = 0.75;
        if (gamepad1.y)
            x = 0.8;

        // Crias variáveis a partir do controle.
        double leftX = gamepad1.left_stick_x;
        double leftY = gamepad1.left_stick_y;
        double rightX = gamepad1.right_stick_x;

        boolean lb = gamepad1.left_bumper;
        boolean rb = gamepad1.right_bumper;

        boolean dpUp = gamepad1.dpad_up;
        boolean dpDown = gamepad1.dpad_down;
        boolean dpLeft = gamepad1.dpad_left;
        boolean dpRight = gamepad1.dpad_right;

        moveT.moveRobot(leftX, leftY, rightX, lb, rb);
        moveT.moveGarra(dpUp, dpDown, dpLeft, dpRight);
        moveT.tiro(gamepad1.a, x);
    }
}