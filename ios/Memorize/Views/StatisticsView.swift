import SwiftUI

struct StatisticsView: View {
    let sessionId: String
    let onBack: () -> Void
    
    @State private var showCelebration = true
    @State private var scale: CGFloat = 1.0
    
    var body: some View {
        VStack(spacing: 16) {
            if showCelebration {
                CelebrationAnimation(scale: $scale)
            }
            
            Text("🎉 Поздравляем! 🎉")
                .font(.largeTitle)
                .fontWeight(.bold)
                .foregroundColor(.primary)
            
            Text("Вы успешно выучили текст!")
                .font(.title2)
            
            Spacer()
            
            StatisticsCard(
                timeSpent: "15:30",
                repetitions: 42,
                mistakes: 8,
                grade: 85.5
            )
            
            Spacer()
            
            Button("Вернуться к поиску") {
                onBack()
            }
            .buttonStyle(.borderedProminent)
            .frame(maxWidth: .infinity)
        }
        .padding()
        .onAppear {
            withAnimation(.easeInOut(duration: 0.5).repeatForever(autoreverses: true)) {
                scale = 1.2
            }
            
            DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                showCelebration = false
            }
        }
    }
}

struct CelebrationAnimation: View {
    @Binding var scale: CGFloat
    
    var body: some View {
        ZStack {
            Circle()
                .fill(
                    RadialGradient(
                        colors: [.yellow, .orange, .red],
                        center: .center,
                        startRadius: 0,
                        endRadius: 100
                    )
                )
                .frame(width: 200, height: 200)
            
            Text("⭐")
                .font(.system(size: 80 * scale))
        }
    }
}

struct StatisticsCard: View {
    let timeSpent: String
    let repetitions: Int
    let mistakes: Int
    let grade: Double
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Статистика")
                .font(.title2)
                .fontWeight(.bold)
            
            StatisticRow(label: "Время обучения:", value: timeSpent)
            StatisticRow(label: "Количество повторов:", value: "\(repetitions)")
            StatisticRow(label: "Количество ошибок:", value: "\(mistakes)")
            StatisticRow(label: "Оценка:", value: "\(Int(grade))%")
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(radius: 4)
    }
}

struct StatisticRow: View {
    let label: String
    let value: String
    
    var body: some View {
        HStack {
            Text(label)
            Spacer()
            Text(value)
                .fontWeight(.bold)
        }
    }
}

#Preview {
    StatisticsView(sessionId: "test", onBack: {})
}

