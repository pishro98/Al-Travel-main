const fs = require('fs');
const content = fs.readFileSync('app/src/main/java/com/example/MainActivity.kt', 'utf-8');

const importsContent = content.split('// --- Travel Data Models ---')[0].split('\n').filter(l => l.startsWith('import ')).join('\n');

const modelRegex = /\/\/ --- Travel Data Models ---\s*\n([\s\S]*?)\n\/\/ --- ViewModel ---/m;
const modelContentMatch = content.match(modelRegex);
const modelContent = modelContentMatch ? modelContentMatch[1] : '';

const viewmodelRegex = /\/\/ --- ViewModel ---\s*\n([\s\S]*?)\n\/\/ --- UI Components ---/m;
const viewmodelContentMatch = content.match(viewmodelRegex);
const viewmodelContent = viewmodelContentMatch ? viewmodelContentMatch[1] : '';

const uiRegex = /\/\/ --- UI Components ---\s*\n([\s\S]*?)\n\/\/ --- Main App Logic ---/m;
const uiContentMatch = content.match(uiRegex);
const uiContent = uiContentMatch ? uiContentMatch[1] : '';

const modelFile = `package com.example.model\n\nimport kotlinx.serialization.Serializable\n\n${modelContent}\n`;
fs.mkdirSync('app/src/main/java/com/example/model', { recursive: true });
// fs.writeFileSync('app/src/main/java/com/example/model/TravelModels.kt', modelFile);

const viewmodelFile = `package com.example\n\n${importsContent}\nimport com.example.model.*\n\n${viewmodelContent}\n`;
fs.writeFileSync('app/src/main/java/com/example/TravelViewModel.kt', viewmodelFile);

const uiFile = `package com.example.ui\n\n${importsContent}\nimport com.example.model.*\nimport com.example.*\n\n${uiContent}\n`;
fs.writeFileSync('app/src/main/java/com/example/ui/TravelComponents.kt', uiFile);

const mainFile = `package com.example\n\nimport android.os.Bundle\nimport androidx.activity.ComponentActivity\nimport androidx.activity.compose.setContent\nimport androidx.activity.enableEdgeToEdge\nimport androidx.compose.foundation.layout.fillMaxSize\nimport androidx.compose.material3.MaterialTheme\nimport androidx.compose.material3.Surface\nimport androidx.compose.ui.Modifier\nimport androidx.lifecycle.viewmodel.compose.viewModel\nimport com.example.ui.theme.MyApplicationTheme\nimport com.example.ui.MainApp\n\nclass MainActivity : ComponentActivity() {\n    override fun onCreate(savedInstanceState: Bundle?) {\n        super.onCreate(savedInstanceState)\n        enableEdgeToEdge()\n        setContent {\n            MyApplicationTheme {\n                Surface(\n                    modifier = Modifier.fillMaxSize(),\n                    color = MaterialTheme.colorScheme.background\n                ) {\n                    val viewModel: TravelViewModel = viewModel()\n                    MainApp(viewModel)\n                }\n            }\n        }\n    }\n}\n`;
fs.writeFileSync('app/src/main/java/com/example/MainActivity.kt', mainFile);

console.log("Extraction complete!");
