
document.addEventListener("DOMContentLoaded", () => {

    const approvalLineSelect = document.getElementById("approvalLine");
    const preview = document.getElementById("approvalLinePreview");

    const draftButton = document.getElementById("draftButton");
    const submitButton = document.getElementById("submitButton");


    const documentTemplate = document.getElementById("documentTemplate");
    const documentContent = document.getElementById("documentContent");


    if (!approvalLineSelect || !preview) {
        return;
    }


    approvalLineSelect.addEventListener("change",async () => {
            const lineId = approvalLineSelect.value;

            if (!lineId) {
                renderEmpty(preview, "결재라인을 선택해주세요.");
                return;
            }

            renderEmpty(preview,"결재라인을 불러오는 중입니다...");

            try {
                const response = await fetch(`/approval-lines/api/${lineId}`);

                if (!response.ok) {
                    throw new Error("결재라인 조회에 실패했습니다.");
                }

                const line = await response.json();

                renderApprovers(preview,line.approvers || []
                );

            } catch (error) {
                console.error(error);
                renderEmpty(
                    preview, "결재라인을 불러오지 못했습니다."
                );
            }
        }
    );


    documentTemplate?.addEventListener("change", () => {
        setText(documentTemplate, documentContent);
    });
});

function renderApprovers(container, approvers) {
    container.innerHTML = "";

    if (!approvers.length) {
        renderEmpty( container,
            "등록된 결재자가 없습니다."
        );

        return;
    }

    const sortedApprovers = [...approvers].sort((a, b) => a.approvalOrder - b.approvalOrder);

    sortedApprovers.forEach(
        (approver, index) => {
            const order = approver.approvalOrder || index + 1;

            const step = document.createElement("div");
            step.className = "approver-step";

            const number = document.createElement("span");
            number.className = "step-number";
            number.textContent = order;

            const info = document.createElement("div");
            info.className = "step-info";

            const name = document.createElement("div");
            name.className = "step-name";
            name.textContent = approver.emplName;

            const sub = document.createElement("div");

            sub.className = "step-sub";


            const employeeInfo = [
                approver.department,
                approver.position
            ].filter(Boolean).join(" · ");

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


    /*
 * 임시저장
 */
    draftButton?.addEventListener("click", async () => {

            const data =
                getDocumentData();

            try {

                await sendDocument(
                    "/approvals/api/draft",
                    data
                );

                alert("임시저장되었습니다.");

                location.href = "/approvals";

            } catch (error) {

                console.error(error);

                alert("임시저장에 실패했습니다.");
            }
        }
    );


    /*
     * 상신
     */
    submitButton?.addEventListener("click", async () => {

            const data = getDocumentData();
            console.log(data);

            /*
             * 제목 검사
             */
            if (!data.title) {

                alert("제목을 입력해주세요.");

                document
                    .getElementById("documentTitle")
                    .focus();

                return;
            }


            /*
             * 내용 검사
             */
            if (!data.content) {

                alert("내용을 입력해주세요.");

                document
                    .getElementById("documentContent")
                    .focus();

                return;
            }


            /*
             * ★ 결재라인 검사
             */
            if (!data.approvalLineId) {

                alert("결재라인을 선택해주세요.");

                document
                    .getElementById("approvalLine")
                    .focus();

                return;
            }



            if (!confirm("문서를 상신하시겠습니까?")) {
                return;
            }


            try {

                await sendDocument(
                    "/approvals/api/submit",
                    data
                );

                alert("문서가 상신되었습니다.");

                location.href = "/approvals";

            } catch (error) {

                console.error(error);

                alert("문서 상신에 실패했습니다.");
            }
        }
    );


    /*
     * 화면 -> Request DTO
     */
    function getDocumentData() {

        const lineValue =
            document
                .getElementById("approvalLine")
                .value;


        return {

            title:
                document
                    .getElementById("documentTitle")
                    .value
                    .trim(),

            content:
                document
                    .getElementById("documentContent")
                    .value
                    .trim(),

            approvalLineId:
                lineValue
                    ? Number(lineValue)
                    : null
        };
    }

    //========================================

    /*
     * POST 공통 처리
     */
    async function sendDocument(url,data) {
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

        if (csrfToken && csrfHeader) {
            headers[csrfHeader.content] =
                csrfToken.content;
        }


        const response =
            await fetch(
                url,
                {
                    method: "POST",
                    headers: headers,
                    body: JSON.stringify(data)
                }
            );


        if (!response.ok) {
            const message = await response.text();
            throw new Error(message);
        }

        return await response.json();
    }


}


function renderEmpty(container,message) {
    container.innerHTML = "";
    const empty= document.createElement("div");
    empty.className = "preview-empty";
    empty.textContent = message;
    container.appendChild(empty);
}


function setText(documentTemplate, documentContent){
        const templates = {

            vacation:
                `[휴가 종류]
- 연차 / 반차 / 병가 / 기타

[휴가 기간]
- 시작일 :
- 종료일 :

[휴가 사유]
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

        const selectedTemplate = templates[documentTemplate.value];
        if (!selectedTemplate) {return;}
        documentContent.value = selectedTemplate;
}