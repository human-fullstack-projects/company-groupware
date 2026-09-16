document.addEventListener("DOMContentLoaded", () => {

    setupAttachmentSelector({
        inputId: "documentFile",
        listId: "selectedFileList",
        existingCount: 0,
        maxTotalFiles: 5
    });


    const approvalLineSelect =
        document.getElementById("approvalLine");

    const preview =
        document.getElementById("approvalLinePreview");

    const draftButton =
        document.getElementById("draftButton");

    const submitButton =
        document.getElementById("submitButton");


    const documentTemplate =
        document.getElementById("documentTemplate");

    const documentContent =
        document.getElementById("documentContent");


    /*
     * 휴가 신청 관련 요소
     */
    const annualLeaveSection =
        document.getElementById("annualLeaveSection");

    const leaveType =
        document.getElementById("leaveType");

    const leaveStartDate =
        document.getElementById("leaveStartDate");

    const leaveEndDate =
        document.getElementById("leaveEndDate");

    const leaveEndDateGroup =
        document.getElementById("leaveEndDateGroup");

    const leaveDaysPreview =
        document.getElementById("leaveDaysPreview");


    if (!approvalLineSelect || !preview) {
        return;
    }


    /*
     * 결재라인 선택
     */
    approvalLineSelect.addEventListener(
        "change",
        async () => {

            const lineId =
                approvalLineSelect.value;


            if (!lineId) {

                renderEmpty(
                    preview,
                    "결재라인을 선택해주세요."
                );

                return;
            }


            renderEmpty(
                preview,
                "결재라인을 불러오는 중입니다..."
            );


            try {

                const response =
                    await fetch(
                        `/approval-lines/api/${lineId}`
                    );


                if (!response.ok) {

                    throw new Error(
                        "결재라인 조회에 실패했습니다."
                    );
                }


                const line =
                    await response.json();


                renderApprovers(
                    preview,
                    line.approvers || []
                );


            } catch (error) {

                console.error(error);

                renderEmpty(
                    preview,
                    "결재라인을 불러오지 못했습니다."
                );
            }
        }
    );


    /*
     * 문서 양식 선택
     */
    documentTemplate?.addEventListener(
        "change",
        () => {

            setText(
                documentTemplate,
                documentContent
            );

            updateAnnualLeaveSection();
        }
    );


    /*
     * 휴가 종류 변경
     */
    leaveType?.addEventListener(
        "change",
        () => {

            updateLeaveTypeUI();
            updateLeaveDaysPreview();
        }
    );


    /*
     * 시작일 변경
     */
    leaveStartDate?.addEventListener(
        "change",
        () => {

            /*
             * 종료일은 시작일보다
             * 이전 날짜를 선택할 수 없게 함
             */
            if (leaveStartDate.value) {

                leaveEndDate.min =
                    leaveStartDate.value;
            }


            /*
             * 반차인 경우
             * 시작일과 종료일을 같은 날짜로 맞춤
             */
            if (
                leaveType.value === "AM_HALF"
                || leaveType.value === "PM_HALF"
            ) {

                leaveEndDate.value =
                    leaveStartDate.value;
            }


            updateLeaveDaysPreview();
        }
    );


    /*
     * 종료일 변경
     */
    leaveEndDate?.addEventListener(
        "change",
        () => {

            updateLeaveDaysPreview();
        }
    );


    /*
     * 임시저장
     */
    draftButton?.addEventListener(
        "click",
        async () => {

            const data =
                getDocumentData();


            /*
             * 휴가 신청서라면
             * 휴가 정보 검사
             */
            if (
                data.documentType === "VACATION"
                && !validateAnnualLeave(data)
            ) {
                return;
            }


            try {

                const savedDocument =
                    await sendDocument(
                        "/approvals/api/draft",
                        data
                    );


                try {

                    await uploadAttachments(
                        savedDocument.documentId
                    );

                } catch (fileError) {

                    console.error(fileError);

                    alert(
                        "문서는 임시저장되었지만 첨부파일 업로드에 실패했습니다."
                    );

                    location.href =
                        `/approvals/${savedDocument.documentId}`;

                    return;
                }


                alert("임시저장되었습니다.");

                location.href =
                    "/approvals";


            } catch (error) {

                console.error(error);

                alert(
                    "임시저장에 실패했습니다."
                );
            }
        }
    );


    /*
     * 상신
     */
    submitButton?.addEventListener(
        "click",
        async () => {

            const data =
                getDocumentData();


            console.log(data);


            /*
             * 제목 검사
             */
            if (!data.title) {

                alert(
                    "제목을 입력해주세요."
                );

                document
                    .getElementById(
                        "documentTitle"
                    )
                    .focus();

                return;
            }


            /*
             * 내용 검사
             */
            if (!data.content) {

                alert(
                    "내용을 입력해주세요."
                );

                document
                    .getElementById(
                        "documentContent"
                    )
                    .focus();

                return;
            }


            /*
             * 결재라인 검사
             */
            if (!data.approvalLineId) {

                alert(
                    "결재라인을 선택해주세요."
                );

                document
                    .getElementById(
                        "approvalLine"
                    )
                    .focus();

                return;
            }


            /*
             * 휴가 신청서 검사
             */
            if (
                data.documentType === "VACATION"
                && !validateAnnualLeave(data)
            ) {

                return;
            }


            if (
                !confirm(
                    "문서를 상신하시겠습니까?"
                )
            ) {

                return;
            }


            try {

                const savedDocument =
                    await sendDocument(
                        "/approvals/api/submit",
                        data
                    );


                try {

                    await uploadAttachments(
                        savedDocument.documentId
                    );

                } catch (fileError) {

                    console.error(fileError);

                    alert(
                        "문서는 상신되었지만 첨부파일 업로드에 실패했습니다."
                    );

                    console.log(
                        fileError.message
                    );

                    location.href =
                        `/approvals/${savedDocument.documentId}`;

                    return;
                }


                alert(
                    "문서가 상신되었습니다."
                );

                location.href =
                    "/approvals";


            } catch (error) {

                console.error(error);

                alert(
                    "문서 상신에 실패했습니다."
                );
            }
        }
    );


    /*
     * 최초 화면 상태
     */
    updateAnnualLeaveSection();
});


/*
 * 결재자 미리보기 출력
 */
function renderApprovers(
    container,
    approvers
) {

    container.innerHTML = "";


    if (!approvers.length) {

        renderEmpty(
            container,
            "등록된 결재자가 없습니다."
        );

        return;
    }


    const sortedApprovers =
        [...approvers].sort(
            (a, b) =>
                a.approvalOrder
                - b.approvalOrder
        );


    sortedApprovers.forEach(
        (approver, index) => {

            const order =
                approver.approvalOrder
                || index + 1;


            const step =
                document.createElement(
                    "div"
                );

            step.className =
                "approver-step";


            const number =
                document.createElement(
                    "span"
                );

            number.className =
                "step-number";

            number.textContent =
                order;


            const info =
                document.createElement(
                    "div"
                );

            info.className =
                "step-info";


            const name =
                document.createElement(
                    "div"
                );

            name.className =
                "step-name";

            name.textContent =
                approver.emplName;


            const sub =
                document.createElement(
                    "div"
                );

            sub.className =
                "step-sub";


            const employeeInfo = [

                approver.department,
                approver.position

            ]
                .filter(Boolean)
                .join(" · ");


            sub.textContent =
                employeeInfo
                    ? `${order}차 결재자 · ${employeeInfo}`
                    : `${order}차 결재자`;


            info.appendChild(name);
            info.appendChild(sub);

            step.appendChild(number);
            step.appendChild(info);

            container.appendChild(step);
        }
    );
}


/*
 * 화면 -> Request DTO
 */
function getDocumentData() {

    const lineValue =
        document
            .getElementById(
                "approvalLine"
            )
            .value;


    const templateValue =
        document
            .getElementById(
                "documentTemplate"
            )
            .value;


    /*
     * 화면 문서 양식
     * ->
     * ApprovalDocumentType
     */
    const documentTypeMap = {

        vacation:
            "VACATION",

        worklog:
            "WORKLOG",

        proposal:
            "PROPOSAL"
    };


    const documentType =
        documentTypeMap[
            templateValue
            ]
        || "GENERAL";


    const data = {

        title:
            document
                .getElementById(
                    "documentTitle"
                )
                .value
                .trim(),

        content:
            document
                .getElementById(
                    "documentContent"
                )
                .value
                .trim(),

        approvalLineId:
            lineValue
                ? Number(lineValue)
                : null,

        documentType:
        documentType
    };


    /*
     * 휴가 신청서일 때만
     * annualLeave를 포함
     */
    if (
        documentType === "VACATION"
    ) {

        data.annualLeave = {

            leaveType:
            document
                .getElementById(
                    "leaveType"
                )
                .value,

            startDate:
                document
                    .getElementById(
                        "leaveStartDate"
                    )
                    .value
                || null,

            endDate:
                document
                    .getElementById(
                        "leaveEndDate"
                    )
                    .value
                || null
        };
    }


    return data;
}


/*
 * 휴가 신청서 영역
 * 표시 / 숨김
 */
function updateAnnualLeaveSection() {

    const documentTemplate =
        document.getElementById(
            "documentTemplate"
        );

    const annualLeaveSection =
        document.getElementById(
            "annualLeaveSection"
        );


    if (
        !documentTemplate
        || !annualLeaveSection
    ) {

        return;
    }


    const isVacation =
        documentTemplate.value
        === "vacation";


    annualLeaveSection.style.display =
        isVacation
            ? "block"
            : "none";


    if (isVacation) {

        updateLeaveTypeUI();
        updateLeaveDaysPreview();
    }
}


/*
 * 연차 / 반차에 따라
 * 날짜 입력 UI 변경
 */
function updateLeaveTypeUI() {

    const leaveType =
        document.getElementById(
            "leaveType"
        );

    const leaveStartDate =
        document.getElementById(
            "leaveStartDate"
        );

    const leaveEndDate =
        document.getElementById(
            "leaveEndDate"
        );

    const leaveEndDateGroup =
        document.getElementById(
            "leaveEndDateGroup"
        );


    if (
        !leaveType
        || !leaveStartDate
        || !leaveEndDate
        || !leaveEndDateGroup
    ) {

        return;
    }


    const isHalfDay =
        leaveType.value === "AM_HALF"
        || leaveType.value === "PM_HALF";


    /*
     * 반차는 하루만 사용하므로
     * 종료일 입력을 숨김
     */
    if (isHalfDay) {

        leaveEndDateGroup.style.display =
            "none";


        if (leaveStartDate.value) {

            leaveEndDate.value =
                leaveStartDate.value;
        }

    } else {

        leaveEndDateGroup.style.display =
            "block";
    }
}


/*
 * 화면용 예상 사용 일수 계산
 *
 * 실제 DB 저장값은
 * 서버에서 다시 계산할 예정
 */
function updateLeaveDaysPreview() {

    const leaveType =
        document.getElementById(
            "leaveType"
        );

    const startInput =
        document.getElementById(
            "leaveStartDate"
        );

    const endInput =
        document.getElementById(
            "leaveEndDate"
        );

    const preview =
        document.getElementById(
            "leaveDaysPreview"
        );


    if (
        !leaveType
        || !startInput
        || !endInput
        || !preview
    ) {

        return;
    }


    /*
     * 반차
     */
    if (
        leaveType.value === "AM_HALF"
        || leaveType.value === "PM_HALF"
    ) {

        if (!startInput.value) {

            preview.textContent =
                "-";

            return;
        }


        preview.textContent =
            "0.5일";

        return;
    }


    /*
     * 연차
     */
    if (
        !startInput.value
        || !endInput.value
    ) {

        preview.textContent =
            "-";

        return;
    }


    const start =
        parseDate(
            startInput.value
        );

    const end =
        parseDate(
            endInput.value
        );


    if (
        !start
        || !end
        || end < start
    ) {

        preview.textContent =
            "-";

        return;
    }


    /*
     * 현재 단계에서는
     * 시작일 ~ 종료일의 달력 날짜 수를 표시
     *
     * 주말 제외 계산은
     * 서버 저장 로직에서 처리
     */
    const millisecondsPerDay =
        24 * 60 * 60 * 1000;


    const difference =
        Math.floor(
            (
                end.getTime()
                - start.getTime()
            )
            / millisecondsPerDay
        )
        + 1;


    preview.textContent =
        `${difference.toFixed(1)}일`;
}


/*
 * yyyy-MM-dd 문자열을
 * 로컬 Date 객체로 변환
 */
function parseDate(value) {

    if (!value) {
        return null;
    }


    const parts =
        value
            .split("-")
            .map(Number);


    if (parts.length !== 3) {
        return null;
    }


    return new Date(
        parts[0],
        parts[1] - 1,
        parts[2]
    );
}


/*
 * 휴가 신청 입력값 검사
 */
function validateAnnualLeave(data) {

    if (!data.annualLeave) {

        alert(
            "휴가 정보를 입력해주세요."
        );

        return false;
    }


    if (!data.annualLeave.leaveType) {

        alert(
            "휴가 종류를 선택해주세요."
        );

        document
            .getElementById(
                "leaveType"
            )
            .focus();

        return false;
    }


    if (!data.annualLeave.startDate) {

        alert(
            "휴가 시작일을 선택해주세요."
        );

        document
            .getElementById(
                "leaveStartDate"
            )
            .focus();

        return false;
    }


    /*
     * 반차
     */
    if (
        data.annualLeave.leaveType
        === "AM_HALF"
        || data.annualLeave.leaveType
        === "PM_HALF"
    ) {

        data.annualLeave.endDate =
            data.annualLeave.startDate;

        return true;
    }


    /*
     * 연차
     */
    if (!data.annualLeave.endDate) {

        alert(
            "휴가 종료일을 선택해주세요."
        );

        document
            .getElementById(
                "leaveEndDate"
            )
            .focus();

        return false;
    }


    if (
        data.annualLeave.endDate
        < data.annualLeave.startDate
    ) {

        alert(
            "종료일은 시작일보다 빠를 수 없습니다."
        );

        document
            .getElementById(
                "leaveEndDate"
            )
            .focus();

        return false;
    }


    return true;
}


/*
 * POST 공통 처리
 */
async function sendDocument(
    url,
    data
) {

    const csrfToken =
        document.querySelector(
            'meta[name="_csrf"]'
        );

    const csrfHeader =
        document.querySelector(
            'meta[name="_csrf_header"]'
        );


    const headers = {
        "Content-Type":
            "application/json"
    };


    if (
        csrfToken
        && csrfHeader
    ) {

        headers[
            csrfHeader.content
            ] =
            csrfToken.content;
    }


    const response =
        await fetch(
            url,
            {
                method:
                    "POST",

                headers:
                headers,

                body:
                    JSON.stringify(data)
            }
        );


    if (!response.ok) {

        const message =
            await response.text();

        throw new Error(
            message
        );
    }


    return await response.json();
}


/*
 * 빈 결재라인 미리보기
 */
function renderEmpty(
    container,
    message
) {

    container.innerHTML =
        "";


    const empty =
        document.createElement(
            "div"
        );


    empty.className =
        "preview-empty";

    empty.textContent =
        message;


    container.appendChild(
        empty
    );
}


/*
 * 문서 양식 기본 내용
 */
function setText(
    documentTemplate,
    documentContent
) {

    const templates = {

        vacation:
            `[휴가 사유]
-

[업무 인수인계]
-`,

        worklog:
            `[업무 일자]
-

[주요 업무 내용]
1.
2.
3.

[진행 상황]
-

[특이사항]
-

[익일 업무 계획]
-`,

        proposal:
            `[품의 일자]
-

[품의 목적]
-

[품의 내용]
-

[예상 비용]
-

[기대 효과]
-

[비고]
-`
    };


    const selectedTemplate =
        templates[
            documentTemplate.value
            ];


    if (!selectedTemplate) {
        return;
    }


    documentContent.value =
        selectedTemplate;
}


/*
 * 첨부파일 업로드
 */
async function uploadAttachments(
    documentId
) {

    const fileInput =
        document.getElementById(
            "documentFile"
        );


    if (
        !fileInput
        || !fileInput.files.length
    ) {

        return;
    }


    const files =
        Array.from(
            fileInput.files
        );


    /*
     * 최대 개수 검사
     */
    if (files.length > 5) {

        throw new Error(
            "첨부파일은 최대 5개까지 가능합니다."
        );
    }


    /*
     * 개별 파일 용량 검사
     */
    for (const file of files) {

        if (
            file.size
            > 10 * 1024 * 1024
        ) {

            throw new Error(
                `${file.name} 파일이 10MB를 초과했습니다.`
            );
        }
    }


    const formData =
        new FormData();


    /*
     * Controller의
     * @RequestParam("files")
     * 와 이름을 맞춤
     */
    files.forEach(
        file => {

            formData.append(
                "files",
                file
            );
        }
    );


    const csrfToken =
        document.querySelector(
            'meta[name="_csrf"]'
        );

    const csrfHeader =
        document.querySelector(
            'meta[name="_csrf_header"]'
        );


    const headers = {};


    if (
        csrfToken
        && csrfHeader
    ) {

        headers[
            csrfHeader.content
            ] =
            csrfToken.content;
    }


    const response =
        await fetch(
            `/approvals/api/${documentId}/attachments`,
            {
                method:
                    "POST",

                headers:
                headers,

                body:
                formData
            }
        );


    if (!response.ok) {

        const message =
            await response.text();


        throw new Error(
            message
            || "첨부파일 업로드에 실패했습니다."
        );
    }
}