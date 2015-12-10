package com.beust.kobalt.maven

import com.beust.kobalt.api.Kobalt

/**
 * Encapsulate a Maven id captured in one string, as used by Gradle or Ivy, e.g. "org.testng:testng:6.9.9".
 * These id's are somewhat painful to manipulate because on top of containing groupId, artifactId
 * and version, they also accept an optional packaging (e.g. "aar") and qualifier (e.g. "no_aop").
 * Determining which is which in an untyped string separated by colons is not clearly defined so
 * this class does a best attempt at deconstructing an id but there's surely room for improvement.
 *
 * This class accepts a versionless id, which needs to end with a :, e.g. "com.beust:jcommander:" (which
 * usually means "latest version") but it doesn't handle version ranges yet.
 */
class MavenId private constructor(val groupId: String, val artifactId: String, val packaging: String?,
        val version: String?) {

    companion object {
        fun isMavenId(id: String) = with(id.split(":")) {
            size == 3 || size == 4
        }

        private fun isVersion(s: String) : Boolean = Character.isDigit(s[0])

        /**
         * Similar to create(MavenId) but don't run IMavenIdInterceptors.
         */
        fun createNoInterceptors(id: String) : MavenId {
            var groupId: String? = null
            var artifactId: String? = null
            var version: String? = null
            var packaging: String? = null
            if (!isMavenId(id)) {
                throw IllegalArgumentException("Illegal id: $id")
            }

            val c = id.split(":")
            groupId = c[0]
            artifactId = c[1]
            if (!c[2].isEmpty()) {
                if (isVersion(c[2])) {
                    version = c[2]
                } else {
                    packaging = c[2]
                    version = c[3]
                }
            }

            return MavenId(groupId, artifactId, packaging, version)
        }

        /**
         * The main entry point to create Maven Id's. Id's created by this function
         * will run through IMavenIdInterceptors.
         */
        fun create(id: String) : MavenId {
            var originalMavenId = createNoInterceptors(id)
            var interceptedMavenId = originalMavenId
            val interceptors = Kobalt.context?.pluginInfo?.mavenIdInterceptors
            if (interceptors != null) {
                interceptedMavenId = interceptors.fold(originalMavenId, {
                            id, interceptor -> interceptor.intercept(id) })
            }

            return interceptedMavenId
        }

        fun create(groupId: String, artifactId: String, packaging: String?, version: String?) =
               create(toId(groupId, artifactId, packaging, version))

        fun toId(groupId: String, artifactId: String, packaging: String? = null, version: String?) =
                "$groupId:$artifactId" +
                    (if (packaging != null) ":$packaging" else "") +
                    ":$version"
    }


    val hasVersion = version != null

    val toId = MavenId.toId(groupId, artifactId, packaging, version)

}
