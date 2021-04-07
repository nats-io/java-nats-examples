package io.nats.examples;

import java.util.Scanner;

class Input {
    int aId = -1;
    int bId = -1;

    @Override
    public String toString() {
        return "Worker A" + aId + ", Worker B" + bId;
    }

    public Input() {
        System.out.println();
        Scanner in = new Scanner(System.in);
        while (aId < 1 || aId > 2) {
            System.out.print("Which Worker Type A, 1 or 2? ");
            aId = in.nextInt();
        }

        while (bId < 1 || bId > 3) {
            System.out.print("Which Worker Type B, 1, 2 or 3? ");
            bId = in.nextInt();
        }
        System.out.println();
    }
}
