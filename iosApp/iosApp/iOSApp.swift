import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        KoinStarterKt.startApp()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
