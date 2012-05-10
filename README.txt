Project Name:
Gyro_pointer

Description:
Control a computer's cursor with a phone's movement.

Initiator:
Falcon Dai

Initial Date:
5/10/2012

Design Principles (and rationale):
0. fast and smooth response (basic for input devices).
1. place user interactions as much as we can on the phone (so a user doesn't need to get up to the computer).
2. security (controling/disturbing the cursor is devastating/annoying).
3. portability (the server side should support different OS's).

Glossary:
client: the app on the phone, the cursor controller
server: the daemon on the computer, the controlled
gyroscope:
rotation vector:

Communication Protocol:
Network
server UDP socket bound to port 50003

Message Structure:
20 bytes
8 bytes timestamp in long type
12 bytes payload


Gesture Design:
Pointer-like mapping of orientation to cursor position.

Message Encrytion Scheme:
