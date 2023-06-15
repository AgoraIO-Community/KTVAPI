Pod::Spec.new do |spec|
  spec.name          = "AgoraKTVAPI"
  spec.version       = "1.0.0.1"
  spec.summary       = "Agora KTV API"
  spec.description   = "iOS library for quickly implement Agora karaoke scenes"
  spec.homepage      = "https://github.com/AgoraIO-Community/KTVAPI.git"
  spec.license       = "MIT"
  spec.author        = { "Agora Lab" => "developer@agora.io" }
  spec.platform      = :ios
  spec.source        = { :git => "https://github.com/AgoraIO-Community/KTVAPI.git", :tag => '1.0.0.1'}
  spec.swift_versions = "5.0"
  spec.source_files  = "Classes/*.swift"
  spec.requires_arc  = true
  spec.ios.deployment_target  = '11.0'
  spec.dependency 'AgoraRtcEngine_Special_iOS', '4.1.1.8'
  spec.pod_target_xcconfig = { 'IPHONEOS_DEPLOYMENT_TARGET' => '11.0' }
  spec.user_target_xcconfig = { 'IPHONEOS_DEPLOYMENT_TARGET' => '11.0' }
end
